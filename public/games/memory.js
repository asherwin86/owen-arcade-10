(function () {
  var FACES = ['🚀', '🐸', '🍕', '🎸', '👾', '🍩', '⚡', '🐙'];

  function mount(root) {
    root.innerHTML =
      '<div class="hud"><span>Moves: <b id="mem-moves">0</b></span><span>Pairs: <b id="mem-pairs">0</b>/8</span></div>' +
      '<div class="mem-grid"></div><p class="hint" id="mem-msg">Find all the matching pairs.</p>' +
      '<button class="small" id="mem-reset">Shuffle</button>';

    var grid = root.querySelector('.mem-grid'), msg = root.querySelector('#mem-msg');
    var cards, open, moves, pairs, locked, timeout;

    function shuffle(a) {
      for (var i = a.length - 1; i > 0; i--) {
        var j = Math.floor(Math.random() * (i + 1)), t = a[i]; a[i] = a[j]; a[j] = t;
      }
      return a;
    }

    function render() {
      grid.innerHTML = '';
      cards.forEach(function (c, i) {
        var el = document.createElement('button');
        el.className = 'mem-card' + (c.up ? ' up' : '') + (c.done ? ' done' : '');
        el.textContent = c.up || c.done ? c.face : '?';
        el.addEventListener('click', function () { flip(i); });
        grid.appendChild(el);
      });
      root.querySelector('#mem-moves').textContent = moves;
      root.querySelector('#mem-pairs').textContent = pairs;
    }

    function flip(i) {
      var c = cards[i];
      if (locked || c.up || c.done) return;
      c.up = true; open.push(i);
      if (open.length === 2) {
        moves++; locked = true;
        var a = cards[open[0]], b = cards[open[1]];
        if (a.face === b.face) {
          a.done = b.done = true; a.up = b.up = false; pairs++; open = []; locked = false;
          if (pairs === 8) msg.textContent = 'You won in ' + moves + ' moves!';
        } else {
          timeout = setTimeout(function () { a.up = b.up = false; open = []; locked = false; render(); }, 700);
        }
      }
      render();
    }

    function reset() {
      clearTimeout(timeout);
      cards = shuffle(FACES.concat(FACES)).map(function (f) { return { face: f, up: false, done: false }; });
      open = []; moves = 0; pairs = 0; locked = false;
      msg.textContent = 'Find all the matching pairs.';
      render();
    }

    root.querySelector('#mem-reset').addEventListener('click', reset);
    reset();
    return function destroy() { clearTimeout(timeout); };
  }

  (window.ArcadeGames = window.ArcadeGames || []).push({ id: 'memory', name: 'Memory Cards', mount: mount });
})();
