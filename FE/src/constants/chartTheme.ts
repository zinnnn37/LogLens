const lineChartTheme = {
  text: {
    fontSize: 11,
    fill: '#0f172a',
  },
  axis: {
    domain: {
      line: {
        stroke: '#e5e7eb',
        strokeWidth: 1,
      },
    },
    ticks: {
      line: {
        stroke: '#e5e7eb',
        strokeWidth: 1,
      },
      text: {
        fontSize: 11,
        fill: '#6b7280',
      },
    },
    legend: {
      text: {
        fontSize: 11,
        fill: '#4b5563',
      },
    },
  },
  grid: {
    line: {
      stroke: '#f3f4f6',
      strokeWidth: 1,
    },
  },
  tooltip: {
    container: {
      background: 'white',
      color: '#0f172a',
      fontSize: 11,
      borderRadius: 6,
      boxShadow: '0 10px 15px -3px rgb(15 23 42 / 0.15)',
      padding: '6px 8px',
    },
  },
  legends: {
    text: {
      fontSize: 11,
      fill: '#4b5563',
    },
  },
} as const;

export { lineChartTheme };
