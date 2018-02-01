package afm424

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import yahoofinance.YahooFinance
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

const val STOCK_RETURN_FILE_NAME = "stockReturn.txt"
const val PORTFOLIO_RETURN_FILE_NAME = "portfolioReturn.txt"
const val DOW_JONES_RETURN_FILE_NAME = "dowJonesReturn.txt"

fun main(args: Array<String>) {
  runBlocking {
    println("Initialising...")
    var dowJones = initDow()
    var portfolio = initPortfolio()

    val job = launch {
      while (true) {
        retryIO {
          portfolio = getReturnPortfolio(portfolio)
        }
        retryIO(times = 3) {
          dowJones = getReturnDowJones(dowJones)
        }
        // Every 24 hours check value of portfolio - preferably after market closes
        delay(24, TimeUnit.HOURS)
      }
    }
    job.join()
  }
}

fun getReturnDowJones(dowJones: DowJones): DowJones {
  println("Getting Dow Jones return...")

  val currentDowJones = YahooFinance.get("^DJI").quote.price.toDouble()
  val percentageReturn = (currentDowJones - dowJones.index) * 100 / dowJones.index

  File(DOW_JONES_RETURN_FILE_NAME).appendText("======= ${LocalDateTime.now()} =======\n")
  File(DOW_JONES_RETURN_FILE_NAME).appendText("OldPrice: ${dowJones.index} NewPrice: $currentDowJones " +
          " Return is ${percentageReturn.format(2)}\n")
  File(DOW_JONES_RETURN_FILE_NAME).appendText("==================\n")

  println("Dowjones return is ${percentageReturn.format(2)}\n")
  dowJones.index = currentDowJones
  return dowJones
}


suspend fun getReturnPortfolio(portfolio: Portfolio): Portfolio {
  val header = "===== Date: ${LocalDateTime.now()} ====="

  println(header)
  println("Getting portfolio return")

  File(STOCK_RETURN_FILE_NAME).appendText(header)

  val oldValue = portfolio.value

  // Get individual stocks price and calculate returns
  // update price of each
  // write to file
  portfolio.stocks.forEach {
    println("Calculating return for: ${it.name}")
    val currentPrice = YahooFinance.get(it.ticker).quote.price.toDouble()
    val percentageReturn = (currentPrice - it.price) * 100 / it.price
    println("OldPrice: ${it.price} CurrentPrice: ${currentPrice.format(2)} " +
            " Return is ${percentageReturn.format(2)}%")

    File(STOCK_RETURN_FILE_NAME).appendText("Stock: ${it.name} Ticker: ${it.ticker} OldPrice: ${it.price} " +
            "CurrentPrice: ${currentPrice.format(2)} " +
            "Return is ${percentageReturn.format(2)}%\n")

    it.price = currentPrice.format(2).toDouble()
    // Small delay between requests - I think yahoo fails if you send too many requests
    delay(2, TimeUnit.SECONDS)
  }

  File(STOCK_RETURN_FILE_NAME).appendText("=======================\n")

  // calculate new value of the portfolio
  // calculate return and update value
  // write to different file
  val newValue = portfolio.stocks.sumByDouble { it.price * it.quantity }
  val portfolioReturn = (newValue - oldValue) * 100 / oldValue

  println("Portfolio Return is ${portfolioReturn.format(2)}")
  File(PORTFOLIO_RETURN_FILE_NAME).appendText("======= ${LocalDateTime.now()} =======\n")
  File(PORTFOLIO_RETURN_FILE_NAME).appendText("OldPrice: $oldValue NewPrice: $newValue " +
          "Return is ${portfolioReturn.format(2)}\n")
  File(PORTFOLIO_RETURN_FILE_NAME).appendText("==================\n")

  portfolio.value = newValue
  println()

  return portfolio
}

fun initPortfolio(): Portfolio {
  val stocks = initStocks()
  val initialValue = stocks.sumByDouble { it.price * it.quantity }
  return Portfolio(stocks, initialValue)
}

// Initial stocks in portfolio and their price/qty
fun initStocks(): ArrayList<Security> {
  val coke = Security("Coke", "KO", 47.86, 2090)
  val verizon = Security("Verizon", "VZ", 54.59, 1832)
  val pfizer = Security("Pfizer", "PFE", 37.03, 2701)
  val ibm = Security("IBM", "IBM", 166.47, 601)
  val exxon = Security("Exxon", "XOM", 88.37, 1132)
  val chevron = Security("Chevron", "CVX", 130.6, 765)
  val merck = Security("Merck & Co.", "MRK", 61.0, 1640)
  val pg = Security("Proctor & Gamble", "PG", 88.23, 1133)
  val ge = Security("General Electric", "GE", 16.335, 6121)
  val cisco = Security("Cisco", "CSCO", 42.19, 2370)

  return arrayListOf(coke, verizon, pfizer, ibm, exxon, chevron, merck, pg, ge, cisco)
}

// Initial Dow jones index
fun initDow(): DowJones {
  return DowJones(26510.0)
}

// data class to represent stocks and portfolio
data class Security(val name: String, val ticker: String, var price: Double, val quantity: Int)
data class Portfolio(val stocks: ArrayList<Security>, var value: Double)
data class DowJones(var index: Double)

// Easy formatter
fun Double.format(digits: Int): String = java.lang.String.format("%.${digits}f", this)

// Retry logic
suspend fun <T> retryIO(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 1000, // 1 second
        maxDelay: Long = 10000,    // 10 second
        factor: Double = 2.0,
        block: suspend () -> T): T {
  var currentDelay = initialDelay
  repeat(times - 1) {
    try {
      return block()
    } catch (e: Exception) {
      println("Exception: $e")
    }
    delay(currentDelay)
    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
  }
  return block() // last attempt
}