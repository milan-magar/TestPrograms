const express = require('express');
const cors = require('cors');
const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

app.post('/select', (req, res) => {
  const { x1, y1, x2, y2 } = req.body;

  if (x1 === undefined || y1 === undefined || x2 === undefined || y2 === undefined) {
    return res.status(400).json({ error: 'Missing coordinates' });
  }

  const cols = 3;
  const rows = 3;
  const cellWidth = (x2 - x1) / cols;
  const cellHeight = (y2 - y1) / rows;

  const cells = [];
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const cx = x1 + cellWidth * (c + 0.5);
      const cy = y1 + cellHeight * (r + 0.5);
      const text = `cell-${r * cols + c + 1}`;
      cells.push({ x: cx, y: cy, text });
    }
  }

  res.json({ cells });
});

app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});