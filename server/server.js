const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'SLM Play Musical AI Server',
    version: '2.4.0',
    timestamp: new Date().toISOString()
  });
});

// Update checker endpoint
app.get('/api/check-update', (req, res) => {
  const currentClientVersion = req.query.currentVersion || '2.4.0';
  const latestVersion = '2.4.0';
  const hasUpdate = latestVersion !== currentClientVersion;

  res.json({
    latestVersion,
    currentClientVersion,
    hasUpdate,
    releaseDate: '2026-09-07',
    changelog: [
      "Jauge de progression Apple Glass Neon haute fluidité",
      "Assistant musical OpenAI connecté pour playlists et vibes intelligentes",
      "Workflow GitHub Actions pour compilation d'APK automatique",
      "Correction et synchronisation instantanée des images d'albums personnalisées",
      "Déverrouillage direct des paramètres de sécurité généraux et isolation du coffre secret",
      "Stabilité temporelle audio sans latence ni décalage"
    ],
    downloadUrl: 'https://github.com/yjoanchris225/SLM-Play/releases/latest/download/SLM-Play.apk'
  });
});

// Assistant OpenAI endpoint
app.post('/api/assistant', async (req, res) => {
  try {
    const { message, userLibrarySummary, apiKey: clientApiKey } = req.body;
    const apiKey = clientApiKey || process.env.OPENAI_API_KEY;

    if (!message) {
      return res.status(400).json({ error: "Le message ne peut pas être vide." });
    }

    const systemPrompt = `Tu es l'assistant musical intelligent officiel de l'application SLM Play (lecteur audio Hi-Fi cyberpunk et studio musical).
Ton rôle :
1. Guider l'utilisateur pour composer des playlists, choisir le bon vibe mood (Cyberpunk, Midnight Chill, Synthwave, Gym Beast, Lofi Dream), et sélectionner des morceaux de sa bibliothèque.
2. Donner des conseils de mastering, d'égalisation et de production sonore.
3. Répondre de manière concise, élégante, passionnée de musique et bien formatée (puces claires, emojis pertinents).
${userLibrarySummary ? `Bibliothèque de l'utilisateur : ${JSON.stringify(userLibrarySummary)}` : ''}`;

    if (!apiKey) {
      // Intelligent fallback responses when no API key is provided
      const lower = message.toLowerCase();
      let reply = "";
      if (lower.includes("sport") || lower.includes("gym") || lower.includes("workout")) {
        reply = "⚡ **Recommandation Gym Beast (128 - 140 BPM)** :\n\nPour maximiser ton énergie, active le profil d'égalisation **Bass Boost + Spatial Stereo** dans le menu SLM !\n- Pistes recommandées : Morceaux Synthwave rapides et rythmiques percutantes.\n- Vibe Mood conseillé : ⚡ Gym Beast.";
      } else if (lower.includes("calme") || lower.includes("dormir") || lower.includes("chill") || lower.includes("relax")) {
        reply = "🌙 **Recommandation Midnight Chill & Sommeil** :\n\n- Active le **Sleep Timer** avec fondu progressif de 30 minutes.\n- Active l'effet **Binaural Ambience** ou pluie relaxante en arrière-plan pour abaisser le rythme cardiaque.\n- Vibe Mood conseillé : 🌙 Midnight Chill.";
      } else if (lower.includes("playlist") || lower.includes("morceau")) {
        reply = "🎵 **Création de Playlist Intelligente** :\n\nJ'ai analysé le tempo de tes morceaux favoris. Je te conseille d'ordonner ta playlist en commençant par un tempo modéré (~105 BPM), d'atteindre le pic d'énergie au milieu (~125 BPM), puis de terminer par une outro planante avec le mode **Gapless Playback** activé pour éviter tout blanc.";
      } else {
        reply = `✨ **SLM Musical AI** : J'ai bien reçu votre demande : « *${message}* ».\n\nPour débloquer la génération GPT-4o illimitée en direct, entrez simplement votre clé API OpenAI dans les paramètres de l'assistant SLM ou définissez OPENAI_API_KEY sur le serveur !`;
      }

      return res.json({
        reply,
        source: 'local_engine',
        moodSuggested: 'SYNTHWAVE'
      });
    }

    // Call OpenAI Chat Completions API
    const response = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`
      },
      body: JSON.stringify({
        model: 'gpt-4o-mini',
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: message }
        ],
        temperature: 0.7,
        max_tokens: 500
      })
    });

    if (!response.ok) {
      const errText = await response.text();
      return res.status(response.status).json({
        error: `Erreur OpenAI (${response.status}): ${errText}`
      });
    }

    const data = await response.json();
    const replyText = data.choices?.[0]?.message?.content || "Aucune réponse reçue.";

    res.json({
      reply: replyText,
      source: 'openai_api',
      model: data.model
    });

  } catch (error) {
    console.error("Server assistant error:", error);
    res.status(500).json({ error: error.message || "Erreur interne du serveur assistant." });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`SLM Play AI Server running on http://0.0.0.0:${PORT}`);
});
