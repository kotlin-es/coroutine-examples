import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.*
import es.kotlin.collection.coroutine.generate
import es.kotlin.net.async.AsyncClient
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.readLine
import es.kotlin.random.get
import es.kotlin.time.TimeSpan
import es.kotlin.time.seconds
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeoutException

// Example showing how to create a tic-tac-toe server without a explicit state machine
// Using await-async coroutine as it's state as an implicit (and cool) state machine
fun main(args: Array<String>) = EventLoop.mainAsync {
	TicTacToe(moveTimeout = 10.seconds).server()
}

suspend fun AsyncClient.println(line: String, charset: Charset = Charsets.UTF_8) = this.write("$line\r\n".toByteArray(charset))
suspend fun Iterable<AsyncClient>.println(line: String, charset: Charset = Charsets.UTF_8) = asyncFun { for (s in this) s.println(line, charset) }

class TicTacToe(val moveTimeout: TimeSpan = 1.seconds, val port: Int = 9090) {
	val server = AsyncServer(port)

	lateinit var connections: AsyncIterator<AsyncClient>

	suspend fun readConnection() = process {
		val player = connections.next()
		player.println("Joined a tic-tac-toe game! Write row and column to place a chip: 00 - 22")
		player
	}

	suspend fun server() = process {
		connections = server.listen().iterator()

		println("Listening at ... $port")

		// Match-making
		while (true) {
			try {
				val player1 = readConnection()
				player1.println("Waiting for other player to start!")
				val player2 = readConnection()
				async {
					// no wait so we continue creating matches while game is running!
					val result = Match(moveTimeout, player1, player2)
					println("Match resolved with $result")
				}
			} catch (t: Throwable) {
				println("Match-making failed! Start over!")
				t.printStackTrace()
			}
		}
	}

	class Match private constructor(val moveTimeout: TimeSpan, val player1: AsyncClient, val player2: AsyncClient, val dummy: Boolean) {
		// Trick!
		companion object {
			operator suspend fun invoke(moveTimeout: TimeSpan, player1: AsyncClient, player2: AsyncClient) = Match(moveTimeout, player1, player2, true).match()
		}

		class PlayerInfo(val id: Int, val chip: Char)

		var turn = 0
		val info = hashMapOf(player1 to PlayerInfo(id = 0, chip = 'X'), player2 to PlayerInfo(id = 1, chip = 'O'))
		val players = listOf(player1, player2)
		fun AsyncClient.info(): PlayerInfo = info[this]!!

		val board = Board()

		suspend private fun readMove(currentPlayer: AsyncClient) = process {
			var pos: Point
			selectmove@ while (true) {
				val line = withTimeout(moveTimeout) { currentPlayer.readLine(this@withTimeout) }
				try {
					val x = ("" + line[0]).toInt()
					val y = ("" + line[1]).toInt()
					pos = Point(x, y)
					if (board.hasChipAt(pos)) {
						currentPlayer.println("Already has a chip: $pos")
						continue
					} else {
						break@selectmove
					}
				} catch (e: Throwable) {
					currentPlayer.println("ERROR: ${e.javaClass}: ${e.message}")
					e.printStackTrace()
				}
			}
			pos
		}

		suspend fun match() = process {
			players.println("tic-tac-toe: Game started!")

			var result: GameResult

			ingame@ while (true) {
				players.println("Turn: $turn")
				board.sendBoardTo(players)
				val currentPlayer = players[turn % players.size]
				currentPlayer.println("Your turn! You have $moveTimeout to move, or a move will perform automatically!")

				val pos = try {
					readMove(currentPlayer)
				} catch (e: TimeoutException) {
					board.getOneAvailablePositions()
				}

				currentPlayer.println("Placed at $pos!")
				board[pos] = currentPlayer.info().chip

				result = board.checkResult()
				when (result) {
					is GameResult.Playing -> {
						turn++
						continue@ingame
					}
					is GameResult.Draw, is GameResult.Win -> {
						board.sendBoardTo(players)
						players.println("$result")
						break@ingame
					}
				}
			}

			players.println("End of game!")
			for (player in players) {
				try {
					player.close()
				} catch (t: Throwable) {

				}
			}

			result
		}
	}

	data class Point(val x: Int, val y: Int)

	class Board {
		val random = Random()

		val data = listOf(
			arrayListOf(' ', ' ', ' '),
			arrayListOf(' ', ' ', ' '),
			arrayListOf(' ', ' ', ' ')
		)

		suspend fun sendBoardTo(players: Iterable<AsyncClient>) = process {
			players.println("---+---+---")
			for (row in data) {
				players.println(" " + row.joinToString(" | "))
				players.println("---+---+---")
			}
		}

		operator fun set(x: Int, y: Int, v: Char) = run { data[y][x] = v }
		operator fun get(x: Int, y: Int) = data[y][x]
		fun hasChipAt(x: Int, y: Int) = this[x, y] != ' '

		operator fun get(p: Point) = this[p.x, p.y]
		operator fun set(p: Point, v: Char) = run { this[p.x, p.y] = v }
		fun hasChipAt(p: Point) = hasChipAt(p.x, p.y)

		fun getAllPositions() = generate {
			for (y in 0 until 3) for (x in 0 until 3) yield(Point(x, y))
		}

		fun getAvailablePositions() = generate {
			for (p in getAllPositions()) if (!hasChipAt(p)) yield(p)
		}

		fun getOneAvailablePositions(): Point = getAvailablePositions().toList()[random]

		fun getRow(y: Int) = (0 until 3).map { x -> Point(x, y) }
		fun getCol(x: Int) = (0 until 3).map { y -> Point(x, y) }
		fun getDiagonal1() = (0 until 3).map { n -> Point(n, n) }
		fun getDiagonal2() = (0 until 3).map { n -> Point(2 - n, n) }

		fun List<Char>.getType(): Char = if (this.all { it == this[0] }) this[0] else ' '

		fun checkResult(): GameResult {
			val pointsList = generate {
				for (n in 0 until 3) {
					yield(getRow(n))
					yield(getCol(n))
				}
				yield(getDiagonal1())
				yield(getDiagonal2())
			}

			for (points in pointsList) {
				val type = points.map { this[it] }.getType()
				if (type != ' ') {
					return GameResult.Win(type, points)
				}
			}

			return if (getAllPositions().map { this[it] }.all { it != ' ' }) {
				GameResult.Draw
			} else {
				GameResult.Playing
			}
		}
	}

	sealed class GameResult {
		object Playing : GameResult()
		object Draw : GameResult()
		data class Win(val type: Char, val positions: List<Point>) : GameResult()
	}

	operator fun <T> Iterable<T>.get(random: Random): T {
		val list = this.toList()
		return list[random.nextInt(list.size)]
	}
}

