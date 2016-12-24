import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.*
import es.kotlin.collection.coroutine.generate
import es.kotlin.net.async.AsyncClient
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.readLineAsync
import es.kotlin.time.seconds
import java.util.*
import java.util.concurrent.TimeoutException

// Example showing how to create a tic-tac-toe server without a explicit state machine
// Using await-async coroutine as it's state as an implicit (and cool) state machine
fun main(args: Array<String>) = EventLoop.mainAsync {
	TicTacToe.server()
}

object TicTacToe {
	val charset = Charsets.UTF_8
	suspend fun AsyncClient.writeLine(line: String) = this.write("$line\r\n".toByteArray(charset))
	suspend fun Iterable<AsyncClient>.writeLine(line: String) = async<Unit> { for (s in this) s.writeLine(line) }

	val server = AsyncServer()
	val port = 9090

	lateinit var connections: AsyncIterator<AsyncClient>

	suspend fun listenConnection() = asyncFun {
		val player = connections.next()
		player.writeLine("Joined a tic-tac-toe game! Write row and column to place a chip: 00 - 22")
		player
	}

	suspend fun server() = asyncFun {
		connections = server.listen(port).iterator()

		println("Listeining at ... $port")

		// Match-making
		while (true) {
			try {
				val player1 = listenConnection()
				player1.writeLine("Waiting for other player to start!")
				val player2 = listenConnection()
				async {
					// no wait so we continue creating matches while game is running!
					val result = Match(player1, player2)
					println("Match resolved with $result")
				}
			} catch (t: Throwable) {
				println("Match-making failed! Start over!")
				t.printStackTrace()
			}
		}
	}

	class Match private constructor(val player1: AsyncClient, val player2: AsyncClient, val dummy: Boolean) {
		// Trick!
		companion object {
			operator suspend fun invoke(player1: AsyncClient, player2: AsyncClient) = Match(player1, player2, true).start()
		}

		class PlayerInfo(val id: Int, val chip: Char)

		var turn = 0
		val info = hashMapOf(player1 to PlayerInfo(id = 0, chip = 'X'), player2 to PlayerInfo(id = 1, chip = 'O'))
		val players = listOf(player1, player2)
		fun AsyncClient.info(): PlayerInfo = info[this]!!

		val board = Board()

		val moveTimeout = 1000.seconds // Almost disable timeout because socket should cancel reading!

		suspend private fun readMove(currentPlayer: AsyncClient) = asyncFun {
			var pos: Point
			selectmove@ while (true) {
				val line = currentPlayer.readLineAsync(charset).awaitWithTimeout(moveTimeout)
				try {
					val x = ("" + line[0]).toInt()
					val y = ("" + line[1]).toInt()
					pos = Point(x, y)
					if (board.hasChipAt(pos)) {
						currentPlayer.writeLine("Already has a chip: $pos")
						continue
					} else {
						break@selectmove
					}
				} catch (e: Throwable) {
					currentPlayer.writeLine("ERROR: ${e.javaClass}: ${e.message}")
					e.printStackTrace()
				}
			}
			pos
		}

		suspend fun start() = asyncFun {
			players.writeLine("tic-tac-toe: Game started!")

			var result: GameResult

			ingame@ while (true) {
				players.writeLine("Turn: $turn")
				board.sendBoard(players)
				val currentPlayer = players[turn % players.size]
				currentPlayer.writeLine("Your turn! You have $moveTimeout to move, or a move will perform automatically!")

				val pos = try {
					readMove(currentPlayer)
				} catch (e: TimeoutException) {
					board.getOneAvailablePositions()
				}

				currentPlayer.writeLine("Placed at $pos!")
				board[pos] = currentPlayer.info().chip

				result = board.checkResult()
				when (result) {
					is GameResult.Playing -> {
						turn++
						continue@ingame
					}
					is GameResult.Draw, is GameResult.Win -> {
						board.sendBoard(players)
						players.writeLine("$result")
						break@ingame
					}
				}
			}

			players.writeLine("End of game!")
			for (player in players) {
				try {
					await(player.closeAsync())
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

		suspend fun sendBoard(players: Iterable<AsyncClient>) = asyncFun {
			players.writeLine("---+---+---")
			for (row in data) {
				players.writeLine(" " + row.joinToString(" | "))
				players.writeLine("---+---+---")
			}
		}

		operator fun set(x: Int, y: Int, v: Char) = run { data[y][x] = v }
		operator fun get(x: Int, y: Int) = data[y][x]
		fun hasChipAt(x: Int, y: Int) = this[x, y] != ' '

		operator fun get(p: Point) = this[p.x, p.y]
		operator fun set(p: Point, v: Char) = run { this[p.x, p.y] = v }
		fun hasChipAt(p: Point) = hasChipAt(p.x, p.y)

		fun getAllPositions() = generate<Point> {
			for (y in 0 until 3) for (x in 0 until 3) {
				yield(Point(x, y))
			}
		}

		fun getAvailablePositions() = generate<Point> {
			for (p in getAllPositions()) if (!hasChipAt(p)) yield(p)
		}

		fun getOneAvailablePositions(): Point = getAvailablePositions()[random]

		fun getRow(y: Int) = (0 until 3).map { x -> Point(x, y) }
		fun getCol(x: Int) = (0 until 3).map { y -> Point(x, y) }
		fun getDiagonal1() = (0 until 3).map { n -> Point(n, n) }
		fun getDiagonal2() = (0 until 3).map { n -> Point(2 - n, n) }

		fun List<Char>.getType(): Char = if (this.all { it == this[0] }) this[0] else ' '

		fun checkResult(): GameResult {
			val pointsList = generate<List<Point>> {
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
		return list[random.nextInt() % list.size]
	}
}

