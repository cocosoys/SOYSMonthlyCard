/* =====================================================================
 * SOYSMonthlyCard 网页共享脚本
 *  - 物品像素图标库（SVG，16x16，image-rendering:pixelated）
 *  - MC 背包槽位渲染 + tooltip
 *  - fetch 封装 / toast
 *  由 index.html（用户侧）与 admin.html（管理 ERP）共同引用。
 * ===================================================================== */
(function () {
  'use strict';

  /* 页面由 /web/plugins/SOYSMonthlyCard/ 托管，API 同源挂在 /api/plugins/SOYSMonthlyCard */
  var API_BASE = '/api/plugins/SOYSMonthlyCard';

  /* ---------------- 像素图标 ---------------- */

  /* 颜色字符 -> 色值 */
  var PAL = {
    K:'#2b2b2b', W:'#ffffff', L:'#cfd6e6', S:'#d7dce6', S2:'#8b93a3',
    D:'#6ff0df', D2:'#23a99a', E:'#55e07a', E2:'#1f9e44',
    G:'#ffd866', G2:'#e3a521', N:'#f6f2dc', N2:'#c9b879',
    P:'#c06fe6', R:'#e8503f', B:'#5a86e6', O:'#ff8f3d', T:'#8a5a32',
    C:'#3ec6da', Y:'#ffe14a', M:'#ff94d8', Q:'#101018', U:'#6a4a28'
  };

  /* 通用：锭（ingot）形状，主色 m / 暗色 h */
  function ingot(m, h) {
    return [
      '................',
      '................',
      '.....KKKKKK.....',
      '....K' + m + m + m + m + 'K....',
      '...K' + m + m + m + m + m + 'K...',
      '..K' + m + m + m + m + m + m + h + 'K..',
      '..K' + m + m + m + m + m + h + h + 'K..',
      '.K' + m + m + m + m + m + h + h + 'KK.',
      '.K' + m + m + m + m + h + h + h + 'KK.',
      '.KK' + m + m + h + h + h + h + 'KK..',
      '..KKKKKKKKKKK...',
      '................'
    ];
  }

  /* 宝石（钻石/绿宝石）形状 */
  function gem(a, b, c) {
    return [
      '................',
      '......KKKK......',
      '.....K' + a + a + 'K.....',
      '....K' + a + a + a + b + 'K....',
      '...K' + a + a + a + b + c + 'K...',
      '..K' + a + a + a + b + c + c + 'K..',
      '.K' + a + a + a + b + c + c + c + 'K.',
      'K' + a + a + a + b + c + c + c + c + 'K',
      '.K' + a + b + c + c + c + c + c + 'K.',
      '..K' + b + c + c + c + c + 'K..',
      '...K' + c + c + c + 'K...',
      '....K' + c + c + 'K....',
      '.....K' + c + 'K.....',
      '......KK.......',
      '................'
    ];
  }

  var ICONS = {
    DIAMOND: gem('W', 'D', 'D2'),
    EMERALD: gem('W', 'E', 'E2'),
    IRON_INGOT: ingot('S', 'S2'),
    GOLD_INGOT: ingot('G', 'G2'),

    NETHER_STAR: [
      '................',
      '.......Y........',
      '......YGY.......',
      '..Y...YGY...Y...',
      '...Y.YGGGGY.Y...',
      '....Y' + 'G' + 'N' + 'N' + 'G' + 'Y....',
      'Y' + 'Y' + 'GG' + 'N' + 'N' + 'N' + 'GG' + 'Y' + 'Y',
      '.Y' + 'G' + 'N' + 'N' + 'W' + 'N' + 'N' + 'G' + 'Y.',
      '..Y' + 'G' + 'N' + 'W' + 'W' + 'N' + 'G' + 'Y..',
      '...Y' + 'G' + 'N' + 'N' + 'N' + 'G' + 'Y...',
      '....Y' + 'G' + 'N' + 'N' + 'G' + 'Y....',
      '...Y.YGGGGY.Y...',
      '..Y...YGY...Y...',
      '......YGY.......',
      '.......Y........',
      '................'
    ],

    TOTEM: [
      '................',
      '....KKKKKK......',
      '...K' + 'T' + 'T' + 'T' + 'T' + 'K.....',
      '...K' + 'T' + 'K' + 'T' + 'K' + 'T' + 'K.....',
      '...K' + 'T' + 'T' + 'T' + 'T' + 'K.....',
      '....K' + 'T' + 'T' + 'T' + 'K......',
      '...K' + 'O' + 'O' + 'O' + 'O' + 'K.....',
      '..K' + 'O' + 'O' + 'G' + 'O' + 'O' + 'K....',
      '..K' + 'O' + 'O' + 'O' + 'O' + 'O' + 'K....',
      '..K' + 'T' + 'T' + 'T' + 'T' + 'T' + 'K....',
      '..K' + 'T' + 'O' + 'T' + 'O' + 'T' + 'K....',
      '..K' + 'T' + 'T' + 'T' + 'T' + 'T' + 'K....',
      '..K' + 'T' + 'T' + 'T' + 'T' + 'T' + 'K....',
      '...KK' + 'T' + 'T' + 'T' + 'KK....',
      '....KKKKKK......',
      '................'
    ],

    DIAMOND_SWORD: [
      '..............D2',
      '.............DD2',
      '............DD.2',
      '...........DD...',
      '..........DD....',
      '.........DD.....',
      '........DD......',
      '.......DD.......',
      '......DD........',
      '.....DD.........',
      '....DD..........',
      '...DD...........',
      '..G2............',
      '.T.G2...........',
      '....T...........',
      '................'
    ],

    ENDER_PEARL: [
      '................',
      '.....KKKKK......',
      '....KDDDDDK.....',
      '...KDDDKDDDK....',
      '..KDDDKKKDDDK...',
      '..KDDKKKKKDDK...',
      '.KDDKKKKKKDDDK..',
      '.KDKKKKKKKKDDK..',
      '.KDDKKKKKKKDDK..',
      '..KDDKKKKKDDK...',
      '..KDDDKKKDDDK...',
      '...KDDDDDDDK....',
      '....KDDDDDK.....',
      '.....KKKKK......',
      '................',
      '................'
    ],

    GOLDEN_APPLE: [
      '................',
      '.....KKKK.......',
      '....KGGGGK......',
      '...KGGGGGGK..R..',
      '..KGGKGGGGGK.R..',
      '..KGGGGGGGGKR...',
      '.KGGGGGGGGGK....',
      '.KGGGGGGGGGK....',
      '.KGGGGGGGGGK....',
      '..KGGGGGGGK.....',
      '..KGGGGGGK......',
      '...KGGGGK.......',
      '....KGGK........',
      '.....KK.........',
      '................',
      '................'
    ],

    EXPERIENCE_BOTTLE: [
      '................',
      '......KKK.......',
      '......K' + 'G' + 'K.......',
      '.....KGGK.......',
      '.....KGGK.......',
      '....KGGGGK......',
      '...KGGGGGGK.....',
      '...KG' + 'Y' + 'GG' + 'Y' + 'GK....',
      '...KGG' + 'Y' + 'Y' + 'GGK....',
      '...KGGGGGGK.....',
      '...KGG' + 'Y' + 'GGGK.....',
      '....KGGGGK......',
      '.....KGGK.......',
      '.....KKKK.......',
      '................',
      '................'
    ],

    CHEST: [
      '................',
      '.KKKKKKKKKKKKKK.',
      '.KUUUUUUUUUUUUK.',
      '.KUUUUUUGUUUUUK.',
      '.KKKKKKKKKKKKKK.',
      '.KGGGGGGGGGGGGK.',
      '.KGGGGGGGGGGGGK.',
      '.KGGGGGGGGGGGGK.',
      '.KGGGGGGKGGGGGK.',
      '.KGGGGGKGGGGGGK.',
      '.KGGGGGGGGGGGGK.',
      '.KKKKKKKKKKKKKK.',
      '................',
      '................',
      '................',
      '................'
    ],

    BOOK: [
      '................',
      '..KKKKKK........',
      '..KRRRRKKKK.....',
      '..KRWRRRRRK.....',
      '..KRWRRRRRK.....',
      '..KRWRRRRRK.....',
      '..KRWRRRRRK.....',
      '..KRWRRRRRK.....',
      '..KRWRRRRRK.....',
      '..KRWRRRRRK.....',
      '..KRWRRRRRK.....',
      '..KRRRRRRRK.....',
      '..KKKKKKKK......',
      '................',
      '................',
      '................'
    ],

    REDSTONE: dot('R', 'K'),
    OBSIDIAN: [
      'KKKKKKKKKKKKKKKK',
      'KQKQKQKKQKQKQKKQ',
      'KKPPKKQKKPPKKQKK',
      'KQKKQKQKQKKQKQKK',
      'KKQKKPPKKQKKPPKK',
      'KQKQKKQKQKQKKQKK',
      'KKPPKKQKKQKKQKPK',
      'KQKKQKQKKPPKKQKK',
      'KKQKKPPKKQKKQKQK',
      'KQKQKKQKQKKPPKKQ',
      'KKPPKKQKKQKKQKPK',
      'KQKKQKQKKQKKQKQK',
      'KKQKKPPKKQKKPPKK',
      'KQKQKKQKQKQKKQKK',
      'KKPPKKQKKQKKQKPK',
      'KKKKKKKKKKKKKKKK'
    ],
    BEDROCK: [
      'KKKKKKKKKKKKKKKK',
      'KQKKQKKKKQKKQKKQ',
      'KKKQKKKKKKQKKKKK',
      'KQKKKKQKKKKKQKKQ',
      'KKKKQKKKKQKKKKKK',
      'KKQKKKKKKKKQKKKK',
      'KQKKKKQKKKKKKQKK',
      'KKKKQKKKKKQKKKKQ',
      'KKQKKKKKKQKKKKKK',
      'KQKKKKQKKKKKKQKK',
      'KKKKQKKKKKKQKKKK',
      'KKQKKKKKKQKKKKKQ',
      'KQKKKKQKKKKQKKKK',
      'KKKKQKKKKKKKKQKK',
      'KKQKKKKKKQKKKKKK',
      'KKKKKKKKKKKKKKKK'
    ]
  };

  function dot(c, k) {
    return [
      '................',
      '.....' + k + k + k + k + '.....',
      '....' + k + c + c + c + k + '....',
      '...' + k + c + c + c + c + c + k + '...',
      '..' + k + c + c + c + c + c + c + k + '..',
      '..' + k + c + c + c + c + c + c + k + '..',
      '..' + k + c + c + c + c + c + c + k + '..',
      '...' + k + c + c + c + c + c + k + '...',
      '....' + k + c + c + c + k + '....',
      '.....' + k + k + k + k + '.....',
      '................',
      '................'
    ];
  }

  /* 把字符矩阵转为 16x16 SVG，放大显示（CSS 控制尺寸） */
  function iconSvg(matrix) {
    var rects = [];
    for (var y = 0; y < matrix.length; y++) {
      var row = matrix[y];
      for (var x = 0; x < row.length; x++) {
        var ch = row.charAt(x);
        if (ch === '.') continue;
        var fill = PAL[ch] || '#888';
        rects.push('<rect x="' + x + '" y="' + y + '" width="1" height="1" fill="' + fill + '"/>');
      }
    }
    return '<svg viewBox="0 0 16 16" shape-rendering="crispEdges" xmlns="http://www.w3.org/2000/svg">'
      + rects.join('') + '</svg>';
  }

  /* ---------------- 颜色 / 文本 ---------------- */

  /* & 颜色码 -> 带色 HTML（用于 tooltip / 名称预览） */
  var MC_COLORS = {
    '0':'#000000','1':'#0000aa','2':'#00aa00','3':'#00aaaa','4':'#aa0000','5':'#aa00aa',
    '6':'#ffaa00','7':'#aaaaaa','8':'#555555','9':'#5555ff','a':'#55ff55','b':'#55ffff',
    'c':'#ff5555','d':'#ff55ff','e':'#ffff55','f':'#ffffff'
  };
  function colorToHtml(text, base) {
    if (text == null) return '';
    var esc = String(text).replace(/&/g, '§');
    var parts = esc.split('§');
    var html = escapeHtml(parts[0]);
    var color = base || '#ffffff';
    for (var i = 1; i < parts.length; i++) {
      var code = parts[i].charAt(0);
      if (MC_COLORS[code]) { color = MC_COLORS[code]; }
      var rest = parts[i].slice(1);
      html += '<span style="color:' + color + '">' + escapeHtml(rest) + '</span>';
    }
    return html;
  }
  function escapeHtml(s) {
    return String(s).replace(/[&<>"]/g, function (c) {
      return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];
    });
  }

  /* ---------------- 物品槽位渲染 ---------------- */

  var tooltipEl = null;

  function itemName(item) {
    if (item.name) return item.name;
    return prettyMaterial(item.material);
  }
  function prettyMaterial(m) {
    if (!m) return '未知物品';
    return m.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, function (c) { return c.toUpperCase(); });
  }

  function renderSlot(item, opts) {
    opts = opts || {};
    var slot = document.createElement('div');
    slot.className = 'slot';
    var px = document.createElement('div');
    px.className = 'px';

    var mat = (item.material || '').toUpperCase();
    var matrix = ICONS[mat];
    if (matrix) {
      px.innerHTML = iconSvg(matrix);
    } else {
      var ph = document.createElement('span');
      ph.className = 'ph';
      ph.textContent = (item.material || '?').charAt(0);
      px.appendChild(ph);
    }
    slot.appendChild(px);

    var amount = parseInt(item.amount, 10) || 1;
    if (amount > 1) {
      var amt = document.createElement('span');
      amt.className = 'amt';
      amt.textContent = amount;
      slot.appendChild(amt);
    }

    bindTooltip(slot, item);
    return slot;
  }

  function bindTooltip(el, item) {
    el.addEventListener('mouseenter', function () { showTooltip(item); });
    el.addEventListener('mousemove', moveTooltip);
    el.addEventListener('mouseleave', hideTooltip);
  }
  function showTooltip(item) {
    if (!tooltipEl) { tooltipEl = document.getElementById('tooltip'); }
    var nameColor = item.name ? '#ffffff' : '#55ff55';
    var html = '<div class="tt-name" style="color:' + nameColor + '">' + colorToHtml(itemName(item)) + '</div>';
    if (item.lore && item.lore.length) {
      html += '<div class="tt-lore">' + item.lore.map(colorToHtml).join('<br>') + '</div>';
    }
    var extra = [];
    if (item.enchants && item.enchants.length) {
      extra = extra.concat(item.enchants.map(function (e) {
        var p = e.split(':');
        return '<span style="color:#bbbbbb">' + prettyEnchant(p[0]) + ' ' + roman(p[1] || 1) + '</span>';
      }));
    }
    if (extra.length) html += '<div class="tt-extra">' + extra.join('<br>') + '</div>';
    if (!item.name) html += '<div class="tt-extra" style="color:#888">' + (item.material || '') + (item.data ? ':' + item.data : '') + '</div>';
    tooltipEl.innerHTML = html;
    tooltipEl.style.display = 'block';
  }
  function moveTooltip(e) {
    if (!tooltipEl) return;
    var x = e.clientX + 14, y = e.clientY + 12;
    var r = tooltipEl.getBoundingClientRect();
    if (x + r.width > window.innerWidth - 8) x = e.clientX - r.width - 14;
    if (y + r.height > window.innerHeight - 8) y = e.clientY - r.height - 12;
    tooltipEl.style.left = x + 'px';
    tooltipEl.style.top = y + 'px';
  }
  function hideTooltip() {
    if (tooltipEl) tooltipEl.style.display = 'none';
  }

  function prettyEnchant(name) {
    var map = {
      DAMAGE_ALL:'锋利', FIRE_ASPECT:'火焰附加', KNOCKBACK:'击退', PROTECTION_ENVIRONMENTAL:'保护',
      PROTECTION_FIRE:'火焰保护', PROTECTION_FALL:'摔落保护', ARROW_DAMAGE:'力量', ARROW_KNOCKBACK:'冲击',
      DURABILITY:'耐久', DIG_SPEED:'效率', LOOT_BONUS_BLOCKS:'时运', SILK_TOUCH:'精准采集',
      LOOT_BONUS_MOBS:'抢夺', DAMAGE_UNDEAD:'亡灵杀手', DAMAGE_ARTHROPODS:'节肢杀手', OXYGEN:'水下呼吸',
      WATER_WORKER:'水下速掘', THORNS:'荆棘', ARROW_FIRE:'火矢', ARROW_INFINITE:'无限'
    };
    return map[name] || name.toLowerCase().replace(/_/g, ' ');
  }
  function roman(n) {
    return ['','I','II','III','IV','V','VI','VII','VIII','IX','X'][n] || n;
  }

  /* ---------------- fetch / toast ---------------- */

  function api(path, options) {
    options = options || {};
    options.credentials = 'same-origin';
    if (options.body && typeof options.body === 'object') {
      options.headers = options.headers || {};
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(options.body);
    }
    return fetch(API_BASE + path, options).then(function (resp) {
      return resp.json().then(function (json) {
        return { ok: resp.ok, status: resp.status, json: json };
      });
    });
  }

  var toastEl = null, toastTimer = null;
  function toast(msg, type) {
    if (!toastEl) toastEl = document.getElementById('toast');
    toastEl.textContent = msg;
    toastEl.className = 'toast show ' + (type || '');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.className = 'toast ' + (type || ''); }, 2600);
  }

  /* ---------------- 导出 ---------------- */
  window.MC = {
    API_BASE: API_BASE,
    ICONS: ICONS,
    iconSvg: iconSvg,
    renderSlot: renderSlot,
    colorToHtml: colorToHtml,
    prettyMaterial: prettyMaterial,
    prettyEnchant: prettyEnchant,
    roman: roman,
    escapeHtml: escapeHtml,
    api: api,
    toast: toast
  };
})();
