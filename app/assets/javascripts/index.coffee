$ ->
  ws = new WebSocket("ws://localhost:9000/ws")
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.type
      when "stockhistory"
        populateStockHistory(message)
      when "stockupdate"
        updateStockChart(message)
      when "stockaverage"        
        console.log("Stock Average!!")
        console.log(message)
      else
        console.log("NOT HANDLED:")
        console.log(message)

  $("#addsymbolform").submit (event) ->
    event.preventDefault()
    # send the message to watch the stock
    ws.send(JSON.stringify({symbol: $("#addsymboltext").val()}))
    # reset the form
    $("#addsymboltext").val("")

  $("#averagestockform").submit (event) ->
    event.preventDefault()
    $.ajax
      method: 'GET'
      url: '/stocks/average'
      success: (data) ->
        window.alert(formatStocksAverage(data))

formatStocksAverage = (averages) ->
  s = ""
  averages.forEach (avg) ->
    s+="Stock: "+avg.symbol + " --- Average: " + Math.round(avg.average) + '\n'
  return s

getPricesFromArray = (data) ->
  (v[1] for v in data)

getChartArray = (data) ->
  ([i, v] for v, i in data)

getChartOptions = (data) ->
  series:
    shadowSize: 0
  yaxis:
    min: getAxisMin(data)
    max: getAxisMax(data)
  xaxis:
    show: false

getAxisMin = (data) ->
  Math.min.apply(Math, data) * 0.9

getAxisMax = (data) ->
  Math.max.apply(Math, data) * 1.1

populateStockHistory = (message) ->
  chart = $("<div>").addClass("chart").prop("id", message.symbol)
  chartHolder = $("<div>").addClass("chart-holder").attr("data-content", message.symbol).append(chart)
  chartHolder.append($("<p>").text("values are simulated"))
  detailsHolder = $("<div>").addClass("details-holder")
  flipper = $("<div>").addClass("flipper").append(chartHolder).append(detailsHolder)
  stockContainer = $("<div>").addClass("flip-container").append(flipper)
  $("#stocks").prepend(stockContainer)
  plot = chart.plot([getChartArray(message.history)], getChartOptions(message.history)).data("plot")

updateStockChart = (message) ->
  if ($("#" + message.symbol).size() > 0)
    plot = $("#" + message.symbol).data("plot")
    data = getPricesFromArray(plot.getData()[0].data)
    data.shift()
    data.push(message.price)
    plot.setData([getChartArray(data)])
    # update the yaxes if either the min or max is now out of the acceptable range
    yaxes = plot.getOptions().yaxes[0]
    if ((getAxisMin(data) < yaxes.min) || (getAxisMax(data) > yaxes.max))
      # reseting yaxes
      yaxes.min = getAxisMin(data)
      yaxes.max = getAxisMax(data)
      plot.setupGrid()
    # redraw the chart
    plot.draw()