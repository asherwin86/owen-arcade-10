(function () {
  var LINES = [[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]];

  function winner(b) {
    for (var i = 0; i < LINES.length; i++) {
      var l = LINES[i];
      if (b[l[0]] && b[l[0]] === b[l[1]] && b[l[0]] === b[l[2]]) return { who: b[l[0]], line: l };
    }
    return null;
  }

  // Unbeatable computer player (O); X is the human.
  function minimax(b, isO) {
    var w = winner(b);
    if (w) return w.who === 'O' ? 1 : -1;
    if (b.every(Boolean)) return 0;
    var best = isO ? -2 : 2;
    for (var i = 0; i < 9; i++) {
      if (b[i]) continue;
      b[i] = isO ? 'O' : 'X';
      var s = minimax(b, !isO);
      b[i] = null;
      best = isO ? Math.max(best, s) : Math.min(best, s);
    }
    return best;
  }

  function aiMove(b) {
    var bestScore = -2, move = -1;
    for (var i = 0; i < 9; i++) {
      if (b[i]) continue;
      b[i] = 'O';
      var s = minimax(b, false);
      b[i] = null;
      if (s > bestScore) { bestScore = s; move = i; }
    }
    return move;
  }

  function mount(root) {
    root.innerHTML =
      '<div class="hud"><span>You: <b id="ttt-w">0</b></span><span>Draws: <b id="ttt-d">0</b></span><span>Computer: <b id="ttt-l">0</b></span></div>' +
      '<div class="ttt-board"></div><p class="hint" id="ttt-msg">You are X. Your move.</p>' +
      '<button class="small" id="ttt-reset">New round</button>';

    var boardEl = root.querySelector('.ttt-board'), msg = root.querySelector('#ttt-msg');
    var stats = { w: 0, d: 0, l: 0 }, board, over, timeout;

    function render(win) {
      boardEl.innerHTML = '';
      board.forEach(function (v, i) {
        var c = document.createElement('button');
        c.className = 'ttt-cell' + (win && win.line.indexOf(i) >= 0 ? ' win' : '');
        c.textContent = v || '';
        c.addEventListener('click', function () { play(i); });
        boardEl.appendChild(c);
      });
    }

    function finish() {
      var w = winner(board);
      if (w) {
        over = true;
        if (w.who === 'X') { stats.w++; msg.textContent = 'You win!'; } else { stats.l++; msg.textContent = 'Computer wins.'; }
      } else if (board.every(Boolean)) {
        over = true; stats.d++; msg.textContent = "It's a draw.";
      }
      root.querySelector('#ttt-w').textContent = stats.w;
      root.querySelector('#ttt-d').textContent = stats.d;
      root.querySelector('#ttt-l').textContent = stats.l;
      render(w);
      return over;
    }

    function play(i) {
      if (over || board[i]) return;
      board[i] = 'X';
      render();
      if (finish()) return;
      msg.textContent = 'Computer is thinking...';
      timeout = setTimeout(function () {
        board[aiMove(board)] = 'O';
        if (!finish()) msg.textContent = 'Your move.';
      }, 350);
    }

    function reset() {
      clearTimeout(timeout);
      board = Array(9).fill(null); over = false;
      msg.textContent = 'You are X. Your move.';
      render();
    }

    root.querySelector('#ttt-reset').addEventListener('click', reset);
    reset();
    return function destroy() { clearTimeout(timeout); };
  }

  (window.ArcadeGames = window.ArcadeGames || []).push({ id: 'tictactoe', name: 'Tic-Tac-Toe', mount: mount });
})();
