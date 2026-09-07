import "dotenv/config";

import express from "express";
import cors from "cors";
import helmet from "helmet";

import authRoutes from "./src/routes/auth.js";
import musicRoutes from "./src/routes/music.js";
import aiRoutes from "./src/routes/ai.js";
import cloudRoutes from "./src/routes/cloud.js";

const app = express();

const PORT = Number(process.env.PORT || 3001);

app.disable("x-powered-by");

app.use(
  helmet()
);

app.use(
  cors({
    origin: process.env.CLIENT_ORIGIN
      ? process.env.CLIENT_ORIGIN
          .split(",")
          .map(origin => origin.trim())
      : true,
    credentials: true
  })
);

app.use(
  express.json({
    limit: "2mb"
  })
);

/*
|--------------------------------------------------------------------------
| HEALTH
|--------------------------------------------------------------------------
*/

app.get("/health", (req, res) => {
  res.json({
    status: "ok",
    service: "SLM Play Server",
    version: "3.0.0",
    openai: Boolean(process.env.OPENAI_API_KEY),
    time: new Date().toISOString()
  });
});

/*
|--------------------------------------------------------------------------
| API
|--------------------------------------------------------------------------
*/

app.use("/api/auth", authRoutes);

app.use("/api/music", musicRoutes);

app.use("/api/ai", aiRoutes);

app.use("/api/cloud", cloudRoutes);

/*
|--------------------------------------------------------------------------
| 404
|--------------------------------------------------------------------------
*/

app.use((req, res) => {
  res.status(404).json({
    error: "Route introuvable"
  });
});

/*
|--------------------------------------------------------------------------
| ERROR HANDLER
|--------------------------------------------------------------------------
*/

app.use((err, req, res, next) => {
  console.error(err);

  res.status(err.status || 500).json({
    error: err.message || "Erreur serveur"
  });
});

/*
|--------------------------------------------------------------------------
| START
|--------------------------------------------------------------------------
*/

app.listen(PORT, "0.0.0.0", () => {
  console.log("");
  console.log("================================");
  console.log("       SLM PLAY SERVER");
  console.log("================================");
  console.log(`PORT: ${PORT}`);
  console.log(`http://localhost:${PORT}`);
  console.log("");
});
