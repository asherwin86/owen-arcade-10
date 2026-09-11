const express = require('express');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// In-memory store for leaderboards
// Format: { "gameId": [ { playerName: "User1", score: 100, date: "..." }, ... ] }
const leaderboards = {};

app.get('/', (req, res) => {
    res.send(`
        <h1>Arcade 10 API</h1>
        <p>The global leaderboard backend is running!</p>
        <p>Endpoints:</p>
        <ul>
            <li><code>GET /api/leaderboard/:gameId</code> - Get top scores for a game</li>
            <li><code>POST /api/leaderboard/:gameId</code> - Submit a new score { playerName, score }</li>
        </ul>
    `);
});

// GET top scores for a specific game
app.get('/api/leaderboard/:gameId', (req, res) => {
    const gameId = req.params.gameId;
    const scores = leaderboards[gameId] || [];
    // Return top 10 scores
    res.json(scores.slice(0, 10));
});

// POST a new score
app.post('/api/leaderboard/:gameId', (req, res) => {
    const gameId = req.params.gameId;
    const { playerName, score } = req.body;

    if (!playerName || typeof score !== 'number') {
        return res.status(400).json({ error: 'Invalid data. Requires playerName (string) and score (number).' });
    }

    if (!leaderboards[gameId]) {
        leaderboards[gameId] = [];
    }

    // Add the new score
    leaderboards[gameId].push({ 
        playerName, 
        score, 
        date: new Date().toISOString() 
    });

    // Sort descending by score
    leaderboards[gameId].sort((a, b) => b.score - a.score);
    
    // Keep only the top 100 to save memory
    leaderboards[gameId] = leaderboards[gameId].slice(0, 100);

    // Return success and the new top 10
    res.json({ 
        success: true, 
        message: 'Score saved successfully!',
        topScores: leaderboards[gameId].slice(0, 10) 
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(\`Arcade 10 Backend API running on port \${PORT}\`);
});
