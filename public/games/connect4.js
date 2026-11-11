(function () {
  var COLS = 7, ROWS = 6;

  // board[c][r]: column c, row r (r = 0 is the bottom). Values: null, 'R' (you), 'Y' (computer).
  function newBoard() {
    var b = [];
    for (var c = 0; c < COLS; c++) b.push(Array(ROWS).fill(null));
    return b;
  }

  function dropRow(b, c) {
    for (var r = 0; r < ROWS; r++) if (!b[c][r]) return r;
    return -1;
  }

  var DIRS = [[1, 0], [0, 1], [1, 1], [1, -1]];

  // Returns the four winning cells as [[c, r], ...] or null.
  function findWin(b) {
    for (var c = 0; c < COLS; c++) {
      for (var r = 0; r < ROWS; r++) {
        var who = b[c][r];
        if (!who) continue;
        for (var d = 0; d < DIRS.length; d++) {
          var cells = [[c, r]];
          for (var k = 1; k < 4; k++) {
            var cc = c + DIRS[d][0] * k, rr = r + DIRS[d][1] * k;
            if (cc < 0 || cc >= COLS || rr < 0 || rr >= ROWS || b[cc][rr] !== who) break;
            cells.push([cc, rr]);
          }
          if (cells.length === 4) return { who: who, cells: cells };
        }
      }
    }
    return null;
  }

  function isFull(b) {
    for (var c = 0; c < COLS; c++) if (dropRow(b, c) >= 0) return false;
    return true;
  }

  // Heuristic score from the computer's (Y) point of view.
  function evaluate(b) {
    var score = 0, c, r;
    for (r = 0; r < ROWS; r++) if (b[3][r] === 'Y') score += 3; else if (b[3][r] === 'R') score -= 3;
    for (c = 0; c < COLS; c++) {
      for (r = 0; r < ROWS; r++) {
        for (var d = 0; d < DIRS.length; d++) {
          var y = 0, red = 0, ok = true;
          for (var k = 0; k < 4; k++) {
            var cc = c + DIRS[d][0] * k, rr = r + DIRS[d][1] * k;
            if (cc < 0 || cc >= COLS || rr < 0 || rr >= ROWS) { ok = false; break; }
            if (b[cc][rr] === 'Y') y++; else if (b[cc][rr] === 'R') red++;
          }
          if (!ok || (y && red)) continue;
          if (y === 3) score += 5; else if (y === 2) score += 2;
          if (red === 3) score -= 6; else if (red === 2) score -= 2;
        }
      }
    }
    return score;
  }

  var ORDER = [3, 2, 4, 1, 5, 0, 6];

  function negamax(b, depth, alpha, beta, who) {
    var w = findWin(b);
    if (w) return (w.who === 'Y' ? 1 : -1) * 100000 * (who === 'Y' ? 1 : -1) * (depth + 1);
    if (depth === 0 || isFull(b)) return (who === 'Y' ? 1 : -1) * evaluate(b);
    var best = -Infinity;
    for (var i = 0; i < ORDER.length; i++) {
      var c = ORDER[i], r = dropRow(b, c);
      if (r < 0) continue;
      b[c][r] = who;
      var s = -negamax(b, depth - 1, -beta, -alpha, who === 'Y' ? 'R' : 'Y');
      b[c][r] = null;
      if (s > best) best = s;
      if (best > alpha) alpha = best;
      if (alpha >= beta) break;
    }
    return best;
  }

  function aiMove(b) {
    var best = -Infinity, move = -1;
    for (var i = 0; i < ORDER.length; i++) {
      var c = ORDER[i], r = dropRow(b, c);
      if (r < 0) continue;
      b[c][r] = 'Y';
      var s = -negamax(b, 4, -Infinity, Infinity, 'R');
      b[c][r] = null;
      if (s > best) { best = s; move = c; }
    }
    return move;
  }

  function mount(root) {
    root.innerHTML =
      '<div class="hud"><span>You: <b id="c4-w">0</b></span><span>Draws: <b id="c4-d">0</b></span><span>Computer: <b id="c4-l">0</b></span></div>' +
      '<div class="c4-board"></div><p class="hint" id="c4-msg">You are red. Pick a column.</p>' +
      '<button class="small" id="c4-reset">New round</button>';

    var boardEl = root.querySelector('.c4-board'), msg = root.querySelector('#c4-msg');
    var stats = { w: 0, d: 0, l: 0 }, board, over, busy, timeout;

    function render(win) {
      boardEl.innerHTML = '';
      // Draw top row first so pieces stack from the bottom.
      for (var r = ROWS - 1; r >= 0; r--) {
        for (var c = 0; c < COLS; c++) {
          var cell = document.createElement('button');
          var v = board[c][r], isWin = false;
          if (win) win.cells.forEach(function (p) { if (p[0] === c && p[1] === r) isWin = true; });
          cell.className = 'c4-cell' + (v === 'R' ? ' red' : v === 'Y' ? ' yellow' : '') + (isWin ? ' win' : '');
          cell.setAttribute('aria-label', 'Column ' + (c + 1));
          cell.addEventListener('click', (function (col) { return function () { play(col); }; })(c));
          boardEl.appendChild(cell);
        }
      }
    }

    function finish() {
      var w = findWin(board);
      if (w) {
        over = true;
        if (w.who === 'R') { stats.w++; msg.textContent = 'You win!'; } else { stats.l++; msg.textContent = 'Computer wins.'; }
      } else if (isFull(board)) {
        over = true; stats.d++; msg.textContent = "It's a draw.";
      }
      root.querySelector('#c4-w').textContent = stats.w;
      root.querySelector('#c4-d').textContent = stats.d;
      root.querySelector('#c4-l').textContent = stats.l;
      render(w);
      return over;
    }

    function play(c) {
      if (over || busy) return;
      var r = dropRow(board, c);
      if (r < 0) return;
      board[c][r] = 'R';
      if (finish()) return;
      busy = true;
      msg.textContent = 'Computer is thinking...';
      timeout = setTimeout(function () {
        var m = aiMove(board);
        board[m][dropRow(board, m)] = 'Y';
        busy = false;
        if (!finish()) msg.textContent = 'Your move.';
      }, 350);
    }

    function reset() {
      clearTimeout(timeout);
      board = newBoard(); over = false; busy = false;
      msg.textContent = 'You are red. Pick a column.';
      render();
    }

    root.querySelector('#c4-reset').addEventListener('click', reset);
    reset();
    return function destroy() { clearTimeout(timeout); };
  }

  (window.ArcadeGames = window.ArcadeGames || []).push({ id: 'connect4', name: 'Connect 4', mount: mount });
})();
