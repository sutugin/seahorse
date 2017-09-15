'use strict';

/* @ngInject */
function ColumnPlot($filter) {

  const maxChartHeight = 400;
  const labelLengthThreshold = 20;
  const maxLabelChars = 40;

  const directive = {
    restrict: 'E',
    templateUrl: 'app/workflows/reports/charts/plot.html',
    replace: true,
    scope: {
      data: '=',
      columnType: '='
    },
    link: function (scope, element) {
      scope.$watch('data', function (data) {
        displayChart(data, element);
      });

      function displayChart(data, element) {
        const labels = getLabels(data.buckets);
        const chart = nv.models.multiBarChart();

        const chartValues = _.map(data.counts, function (val, idx) {
          return {
            x: labels.texts[idx],
            y: val
          };
        });

        let chartData = [{
          values: chartValues,
          key: 'Value occurences',
          color: '#ff7f0e'
        }];

        chart
          .margin({left: 70, right: 70, bottom: 20, top: 20})
          .height(getChartHeight(labels.longest))
          .duration(500)
          .noData('There is no Data to display')
          .groupSpacing(0.03)
          .reduceXTicks(false)
          .showControls(false)
          .color(['#ff7f0e'])
          .rotateLabels(labels.angle);

        d3.select(element[0].querySelector('.svg-plot'))
          .datum(chartData)
          .call(chart);

        nv.utils.windowResize(chart.update);
      }

      function getLabels(labelData) {
        let longestLabel = 0;

        const labelTexts = _.map(_.sliding(labelData, 2), ([start, end]) => {
          if (scope.columnType === 'timestamp') {
            start = moment(new Date(start)).format('YYYY-MM-DD HH:mm:ss');
            end = moment(new Date(end)).format('YYYY-MM-DD HH:mm:ss');
          } else {
            start = $filter('precision')(start);
            end = $filter('precision')(end);
          }

          const str = `${start} - ${end}`;

          if (str.length > longestLabel) {
            longestLabel = str.length;
          }

          return str;
        });

        if (longestLabel > maxLabelChars) {
          longestLabel = maxLabelChars;
        }

        let labelAngle = -45;
        if (longestLabel > labelLengthThreshold) {
          labelAngle = -70;
        }

        return {
          texts: labelTexts,
          longest: longestLabel,
          angle: labelAngle
        };
      }

      function getChartHeight(longestLabel) {
        let letterSize = 4;
        if (longestLabel > labelLengthThreshold) {
          letterSize = 5;
        }

        return maxChartHeight - longestLabel * letterSize;
      }

    }
  };
  return directive;

}

exports.inject = function (module) {
  module.directive('columnPlot', ColumnPlot);
};
