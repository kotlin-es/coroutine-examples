import es.kotlin.async.EventLoop
import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.collection.coroutine.generate
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.AsyncSocket
import es.kotlin.net.async.readLineAsync
import es.kotlin.time.seconds
import java.util.*
import java.util.concurrent.TimeoutException

// Example showing how to create a tic-tac-toe server without a explicit state machine
// Using await-async coroutine as it's state as an implicit (and cool) state machine
fun main(args: Array<String>) = EventLoop.mainAsync {
	TicTacToe.serverAsync().await()
}

object TicTacToe {
	val charset = Charsets.UTF_8
	fun AsyncSocket.writeLineAsync(line: String) = this.writeAsync("$line\r\n".toByteArray(charset))
	fun Iterable<AsyncSocket>.writeLineAsync(line: String) = async<Unit> { for (s in this@writeLineAsync) s.writeLineAsync(line).await() }

	fun serverAsync() = async<Unit> {
		val server = AsyncServer()
		val port = 9090

		val connections = server.listen(port)

		println("Listeining at ... $port")

		fun listenConnectionAsync() = async<AsyncSocket> {
			val player = connections.readOneAsync().await()
			player.writeLineAsync("Joined a tic-tac-toe game! Write row and column to place a chip: 00 - 22").await()
			player
		}

		// Match-making
		while (true) {
			try {
				val player1 = listenConnectionAsync().await()
				player1.writeLineAsync("Waiting for other player to start!").await()
				val player2 = listenConnectionAsync().await()
				val matchPromise = matchAsync(player1, player2) // no wait!
				matchPromise.then { result ->
					println("Match resolved with $result")
				}
			} catch (t: Throwable) {
				println("Match-making failed! Start over!")
				t.printStackTrace()
			}
		}
	}

	fun matchAsync(player1: AsyncSocket, player2: AsyncSocket) = async<GameResult> {
		class PlayerInfo(val id: Int, val chip: Char)

		var turn = 0
		val info = hashMapOf(player1 to PlayerInfo(id = 0, chip = 'X'), player2 to PlayerInfo(id = 1, chip = 'O'))
		val players = listOf(player1, player2)
		fun AsyncSocket.info(): PlayerInfo = info[this]!!

		val board = Board()

		players.writeLineAsync("tic-tac-toe: Game started!").await()

		val moveTimeout = 1000.seconds // Almost disable timeout because socket should cancel reading!

		fun readMoveAsync(currentPlayer: AsyncSocket): Promise<Point> = async {
			var pos: Point
			selectmove@while (true) {
				val line = currentPlayer.readLineAsync(charset).awaitWithTimeout(moveTimeout)
				try {
					val x = ("" + line[0]).toInt()
					val y = ("" + line[1]).toInt()
					pos = Point(x, y)
					if (board.hasChipAt(pos)) {
						currentPlayer.writeLineAsync("Already has a chip: $pos").await()
						continue
					} else {
						break@selectmove
					}
				} catch (e: Throwable) {
					currentPlayer.writeLineAsync("ERROR: ${e.javaClass}: ${e.message}").await()
					e.printStackTrace()
				}
			}
			pos
		}

		var result: GameResult

		ingame@while (true) {
			players.writeLineAsync("Turn: $turn").await()
			board.sendBoardAsync(players).await()
			val currentPlayer = players[turn % players.size]
			currentPlayer.writeLineAsync("Your turn! You have $moveTimeout to move, or a move will perform automatically!").await()

			val pos = try {
				readMoveAsync(currentPlayer).await()
			} catch (e: TimeoutException) {
				board.getOneAvailablePositions()
			}

			currentPlayer.writeLineAsync("Placed at $pos!").await()
			board[pos] = currentPlayer.info().chip

			result = board.checkResult()
			when (result) {
				is GameResult.Playing -> {
					turn++
					continue@ingame
				}
				is GameResult.Draw, is GameResult.Win -> {
					board.sendBoardAsync(players).await()
					players.writeLineAsync("$result").await()
					break@ingame
				}
			}
		}

		players.writeLineAsync("End of game!").await()
		for (player in players) {
			try {
				player.closeAsync().await()
			} catch (t: Throwable) {

			}
		}

		result
	}

	data class Point(val x: Int, val y: Int)

	class Board {
		val random = Random()

		val data = listOf(
			arrayListOf(' ', ' ', ' '),
			arrayListOf(' ', ' ', ' '),
			arrayListOf(' ', ' ', ' ')
		)

		fun sendBoardAsync(players: Iterable<AsyncSocket>) = async<Unit> {
			players.writeLineAsync("---+---+---").await()
			for (row in data) {
				players.writeLineAsync(" " + row.joinToString(" | ")).await()
				players.writeLineAsync("---+---+---").await()
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

