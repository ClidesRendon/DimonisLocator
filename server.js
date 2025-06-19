const express = require('express');
const fs = require('fs');
const http = require('http');
const path = require('path');
const bodyParser = require('body-parser');
const cors = require('cors');

const app = express();
const PORT = 3001;

app.use(cors());
app.use(bodyParser.json());
app.use(express.static('public'));

const LOCATION_FILE = path.join(__dirname, '/location.json');

// Función para guardar ubicación
function guardarUbicacion(lat, lon) {
  let data = [];

  try {
    if (fs.existsSync(LOCATION_FILE)) {
      const fileContent = fs.readFileSync(LOCATION_FILE, 'utf8');
      data = JSON.parse(fileContent);

      // Asegurarnos que es un array
      if (!Array.isArray(data)) {
        data = [];
      }
    }
  } catch (error) {
    console.error('Error leyendo/parsing location.json:', error);
    data = [];
  }

  // Añadir nueva ubicación con timestamp
  data.push({ lat, lon, timestamp: Date.now() });

  try {
    fs.writeFileSync(LOCATION_FILE, JSON.stringify(data, null, 2));
  } catch (error) {
    console.error('Error escribiendo location.json:', error);
  }
}


// Endpoint POST raíz para compatibilidad con la app Android
app.post('/', (req, res) => {
  const { lat, lon } = req.body;
  console.log("📩 Coordenadas recibidas en /:", req.body);

  if (typeof lat === 'number' && typeof lon === 'number') {
    guardarUbicacion(lat, lon);
    res.send({ status: 'ok' });
  } else {
    res.status(400).send({ error: 'Datos inválidos' });
  }
});

// Endpoint opcional para recibir datos en /update-location también
app.post('/update-location', (req, res) => {
  const { lat, lon } = req.body;
  console.log("📩 Coordenadas recibidas en /update-location:", req.body);

  if (typeof lat === 'number' && typeof lon === 'number') {
    guardarUbicacion(lat, lon);
    res.send({ status: 'ok' });
  } else {
    res.status(400).send({ error: 'Datos inválidos' });
  }
});

// Devolver las ubicaciones almacenadas
app.get('/location.json', (req, res) => {
  if (fs.existsSync(LOCATION_FILE)) {
    res.sendFile(LOCATION_FILE);
  } else {
    res.send([]); // Devolver array vacío si no hay datos
  }
});

// Limpiar el archivo location.json
app.post('/clear-location', (req, res) => {
  fs.writeFileSync(LOCATION_FILE, JSON.stringify([]));
  console.log("🧹 Ruta limpiada");
  res.send({ status: 'ruta limpiada' });
});

app.listen(PORT, '0.0.0.0',() => {
  console.log(`🌍 Servidor iniciado en http://localhost:${PORT}`);
});
