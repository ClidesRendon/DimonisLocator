const express = require('express');
const fs = require('fs');
const path = require('path');
const bodyParser = require('body-parser');
const cors = require('cors');

const app = express();
const PORT = 3000;

app.use(cors());
app.use(bodyParser.json());
app.use(express.static('public'));

const LOCATION_FILE = path.join(__dirname, 'location.json');

// Endpoint para recibir ubicación
app.post('/update-location', (req, res) => {
  const { lat, lon } = req.body;
  if (typeof lat === 'number' && typeof lon === 'number') {
    fs.writeFileSync(LOCATION_FILE, JSON.stringify({ lat, lon }));
    res.send({ status: 'ok' });
  } else {
    res.status(400).send({ error: 'Datos inválidos' });
  }
});

// Endpoint para devolver la ubicación
app.get('/location.json', (req, res) => {
  if (fs.existsSync(LOCATION_FILE)) {
    res.sendFile(LOCATION_FILE);
  } else {
    res.send({ lat: 0, lon: 0 });
  }
});

app.listen(PORT, () => {
  console.log(`🌍 Servidor iniciado en http://localhost:${PORT}`);
});
