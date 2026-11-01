(function () {
  var SIZE = 18, CELL = 20;

  function mount(root) {
    root.innerHTML =
      '<div class="hud"><span>Score: <b id="sn-score">0</b></span><span>Best: <b id="sn-best">0</b></span></div>' +
      '<canvas width="' + SIZE * CELL + '" height="' + SIZE * CELL + '" tabindex="0"></canvas>' +
      '<div class="pad"><button data-d="up">&#9650;</button><div><button data-d="left">&#9664;</button>' +
      '<button data-d="down">&#9660;</button><button data-d="right">&#9654;</button></div></div>' +
      '<p class="hint">Arrow keys / WASD, or the on-screen pad. Press Space to restart.</p>';

    var canvas = root.querySelector('canvas'), ctx = canvas.getContext('2d');
    var scoreEl = root.querySelector('#sn-score'), bestEl = root.querySelector('#sn-best');
    var best = 0, snake, dir, nextDir, food, score, dead, timer, waiting;

    function reset() {
      snake = [{ x: 9, y: 9 }, { x: 8, y: 9 }, { x: 7, y: 9 }];
      dir = nextDir = { x: 1, y: 0 };
      score = 0; dead = false;
      placeFood();
      scoreEl.textContent = 0;
      clearInterval(timer);
      waiting = true;
      draw();
    }

    function begin() {
      if (!waiting) return;
      waiting = false;
      timer = setInterval(tick, 120);
    }

    function placeFood() {
      do {
        food = { x: Math.floor(Math.random() * SIZE), y: Math.floor(Math.random() * SIZE) };
      } while (snake.some(function (s) { return s.x === food.x && s.y === food.y; }));
    }

    function tick() {
      dir = nextDir;
      var head = { x: snake[0].x + dir.x, y: snake[0].y + dir.y };
      if (head.x < 0 || head.y < 0 || head.x >= SIZE || head.y >= SIZE ||
          snake.some(function (s) { return s.x === head.x && s.y === head.y; })) {
        dead = true; clearInterval(timer); draw(); return;
      }
      snake.unshift(head);
      if (head.x === food.x && head.y === food.y) {
        score++; scoreEl.textContent = score;
        if (score > best) { best = score; bestEl.textContent = best; }
        placeFood();
      } else {
        snake.pop();
      }
      draw();
    }

    function draw() {
      ctx.fillStyle = '#120e2b'; ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = '#ff5c7a';
      ctx.fillRect(food.x * CELL + 3, food.y * CELL + 3, CELL - 6, CELL - 6);
      snake.forEach(function (s, i) {
        ctx.fillStyle = i === 0 ? '#ffd23f' : '#5be37d';
        ctx.fillRect(s.x * CELL + 1, s.y * CELL + 1, CELL - 2, CELL - 2);
      });
      if (waiting) {
        ctx.fillStyle = 'rgba(0,0,0,.55)'; ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#fff'; ctx.textAlign = 'center';
        ctx.font = 'bold 22px sans-serif'; ctx.fillText('Press a direction to start', canvas.width / 2, canvas.height / 2);
      }
      if (dead) {
        ctx.fillStyle = 'rgba(0,0,0,.65)'; ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#fff'; ctx.textAlign = 'center';
        ctx.font = 'bold 28px sans-serif'; ctx.fillText('Game over', canvas.width / 2, canvas.height / 2);
        ctx.font = '14px sans-serif'; ctx.fillText('Space or tap to restart', canvas.width / 2, canvas.height / 2 + 26);
      }
    }

    var dirs = { up: { x: 0, y: -1 }, down: { x: 0, y: 1 }, left: { x: -1, y: 0 }, right: { x: 1, y: 0 } };
    function turn(name) {
      var d = dirs[name];
      if (d.x === -dir.x && d.y === -dir.y) return;
      nextDir = d;
      begin();
    }

    function onKey(e) {
      var map = { ArrowUp: 'up', w: 'up', ArrowDown: 'down', s: 'down', ArrowLeft: 'left', a: 'left', ArrowRight: 'right', d: 'right' };
      if (map[e.key]) { e.preventDefault(); turn(map[e.key]); }
      else if (e.key === ' ' && dead) { e.preventDefault(); reset(); }
    }
    document.addEventListener('keydown', onKey);
    canvas.addEventListener('click', function () { if (dead) reset(); else begin(); });
    root.querySelectorAll('.pad button').forEach(function (b) {
      b.addEventListener('click', function () { turn(b.dataset.d); });
    });

    reset();
    return function destroy() { clearInterval(timer); document.removeEventListener('keydown', onKey); };
  }

  (window.ArcadeGames = window.ArcadeGames || []).push({ id: 'snake', name: 'Snake', mount: mount });
})();
