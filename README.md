# Portfolio Tracker
For AFM424 - Equity Investments - course at UW - We were tasked with implementing a investing strategy, 
screening stocks that met the criterion for that strategy, and then using $1 million virtual cash to purchase them

We have to go in and track the portfolio return everyday till the end of the semester and the report on the performance of the strategy

Doing this manually was going to be tough as the semester ramps up

Decided to write a small Kotlin script that runs once every 24 hours and gets the data from the yahoo finance api to calculate all the returns
and dumps it into a file

At the end of the semester I will go in and parse out the file to and Excel spreadsheet and just submit that

# Quirks
## 'Run every 24 hours'
Instead of using a ScheduledExecutorTask or a FixedTimerTask - I decided to use a coroutine with `while(true) + delay(1 Day)` 
which makes it a bit more simple and was suggested by Jake Wharton

## RetryIO
Things fail. And I did not want my script crashing halfway through the semester because Yahoo was having issues.
Roman Elizarov from Jetbrains and dev on Kotlin mentioned the following:
```
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
```

The `retryIO` allows me to give it a function and make sure there is an exponential backoff when retrying network calls

## Init

the `initStocks()` and `initDow()` just represent the first day of trading and the initial state of the portfolio when the stocks were bought

this can be changes as needed to whatever [eg changing the dow to S&P500 instead]


# Running

`mvn clean install`

`mvn package` to generate fat jar

`java -jar target/portfolio-tracker-1.0-SNAPSHOT-jar-with-dependencies.jar`

Run the script once after the market closes and it will calculate the returns daily every 24 hours at the same time everyday
