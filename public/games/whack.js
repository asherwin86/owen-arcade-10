(function () {
  var DURATION = 30;

  function mount(root) {
    root.innerHTML =
      '<div class="hud"><span>Score: <b id="wk-score">0</b></span><span>Time: <b id="wk-time">' + DURATION + '</b></span><span>Best: <b id="wk-best">0</b></span></div>' +
      '<div class="wk-grid"></div><p class="hint" id="wk-msg">Hit start, then whack the moles!</p>' +
      '<button class="small" id="wk-start">Start</button>';

    var grid = root.querySelector('.wk-grid'), msg = root.querySelector('#wk-msg');
    var holes = [], score = 0, best = 0, left = DURATION, active = -1, running = false, tickTimer, moleTimer;

    for (var i = 0; i < 9; i++) {
      (function (idx) {
        var h = document.createElement('button');
        h.className = 'wk-hole';
        h.addEventListener('click', function () {
          if (!running || idx !== active) return;
          score++; active = -1; show(); update();
        });
        grid.appendChild(h); holes.push(h);
      })(i);
    }

    function show() {
      holes.forEach(function (h, i) { h.className = 'wk-hole' + (i === active ? ' mole' : ''); h.textContent = i === active ? '🐹' : ''; });
    }

    function update() {
      root.querySelector('#wk-score').textContent = score;
      root.querySelector('#wk-time').textContent = left;
      root.querySelector('#wk-best').textContent = best;
    }

    function popUp() {
      var n;
      do { n = Math.floor(Math.random() * 9); } while (n === active);
      active = n; show();
      moleTimer = setTimeout(popUp, 450 + Math.random() * 450);
    }

    function stop() {
      running = false; clearInterval(tickTimer); clearTimeout(moleTimer);
      active = -1; show();
      if (score > best) best = score;
      msg.textContent = "Time's up! You whacked " + score + '.';
      update();
    }

    function start() {
      clearInterval(tickTimer); clearTimeout(moleTimer);
      score = 0; left = DURATION; running = true;
      msg.textContent = 'Go go go!';
      update(); popUp();
      tickTimer = setInterval(function () { left--; update(); if (left <= 0) stop(); }, 1000);
    }

    root.querySelector('#wk-start').addEventListener('click', start);
    show();
    return function destroy() { clearInterval(tickTimer); clearTimeout(moleTimer); };
  }

  (window.ArcadeGames = window.ArcadeGames || []).push({ id: 'whack', name: 'Whack-a-Mole', mount: mount });
})();
