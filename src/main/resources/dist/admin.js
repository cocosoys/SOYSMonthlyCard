/* =====================================================================
 * 管理后台 admin.js
 *  模块：礼包列表（领取/重置领取/礼品配置/配置/领取记录/启用禁用）、设置、日志
 *  礼品配置：MC 物品栏格子 + 仿 mcmod 物品编辑器 + 读取在线管理员背包拷贝
 * ===================================================================== */
(function () {
  'use strict';
  var MC = window.MC;

  var state = {
    overview: null,
    tiers: [],
    onlinePlayers: [],
    settings: null
  };

  /* 1.12.2 常见附魔（编辑器下拉），值为 Bukkit Enchantment 名 */
  var ENCHANTS = [
    ['DAMAGE_ALL', '锋利'], ['DAMAGE_UNDEAD', '亡灵杀手'], ['DAMAGE_ARTHROPODS', '节肢杀手'],
    ['KNOCKBACK', '击退'], ['FIRE_ASPECT', '火焰附加'], ['LOOT_BONUS_MOBS', '抢夺'],
    ['DIG_SPEED', '效率'], ['SILK_TOUCH', '精准采集'], ['DURABILITY', '耐久'], ['LOOT_BONUS_BLOCKS', '时运'],
    ['PROTECTION_ENVIRONMENTAL', '保护'], ['PROTECTION_FIRE', '火焰保护'], ['PROTECTION_FALL', '摔落保护'],
    ['PROTECTION_EXPLOSIONS', '爆炸保护'], ['PROTECTION_PROJECTILE', '弹射物保护'], ['THORNS', '荆棘'],
    ['OXYGEN', '水下呼吸'], ['WATER_WORKER', '水下速掘'],
    ['ARROW_DAMAGE', '力量'], ['ARROW_KNOCKBACK', '冲击'], ['ARROW_FIRE', '火矢'], ['ARROW_INFINITE', '无限']
  ];

  /* 常见材质（编辑器 datalist 提示，实际可手填任意 1.12.2 材质） */
  var COMMON_MATS = ['DIAMOND','EMERALD','IRON_INGOT','GOLD_INGOT','NETHER_STAR','TOTEM','DIAMOND_SWORD',
    'DIAMOND_PICKAXE','ENDER_PEARL','GOLDEN_APPLE','EXPERIENCE_BOTTLE','CHEST','BOOK','REDSTONE','OBSIDIAN',
    'BEDROCK','APPLE','BREAD','COOKED_BEEF','CAKE','ARROW','BOW','GOLDEN_HELMET','IRON_CHESTPLATE','SKULL_ITEM'];

  /* ============================================================
   * 导航 / 初始化
   * ============================================================ */
  var VIEW_TITLES = { tiers: '礼包列表', settings: '设置', logs: '日志' };

  document.querySelectorAll('.nav-item').forEach(function (item) {
    item.addEventListener('click', function () {
      var view = item.dataset.view;
      document.querySelectorAll('.nav-item').forEach(function (n) { n.classList.remove('active'); });
      item.classList.add('active');
      document.querySelectorAll('.view').forEach(function (v) { v.classList.remove('active'); });
      document.getElementById('view-' + view).classList.add('active');
      document.getElementById('page-title').textContent = VIEW_TITLES[view];
      if (view === 'settings') loadSettings();
      if (view === 'logs') loadLogs();
      if (view === 'tiers') loadTiers();
    });
  });

  document.getElementById('menu-toggle').addEventListener('click', function () {
    document.getElementById('sidebar').classList.toggle('open');
  });

  document.getElementById('btn-refresh-tiers').addEventListener('click', loadTiers);
  document.getElementById('btn-reload-settings').addEventListener('click', loadSettings);
  document.getElementById('btn-refresh-logs').addEventListener('click', loadLogs);

  /* ============================================================
   * Modal 框架
   * ============================================================ */
  var mask = document.getElementById('modal-mask');
  var modalBox = document.getElementById('modal-box');
  var modalTitle = document.getElementById('modal-title');
  var modalBody = document.getElementById('modal-body');
  var modalFoot = document.getElementById('modal-foot');

  function openModal(opts) {
    modalTitle.textContent = opts.title || '';
    modalBox.className = 'modal' + (opts.size === 'lg' ? ' lg' : '');
    modalBody.innerHTML = '';
    modalFoot.innerHTML = '';
    if (opts.content instanceof Node) {
      modalBody.appendChild(opts.content);
    } else {
      modalBody.innerHTML = opts.content || '';
    }
    (opts.buttons || []).forEach(function (b) {
      var btn = document.createElement('button');
      btn.className = 'btn ' + (b.cls || '');
      btn.textContent = b.label;
      btn.addEventListener('click', function () {
        if (b.onClick) b.onClick(closeModal);
        else closeModal();
      });
      modalFoot.appendChild(btn);
    });
    mask.classList.add('show');
    if (opts.onMount) opts.onMount(modalBody);
  }
  function closeModal() { mask.classList.remove('show'); }
  document.getElementById('modal-close').addEventListener('click', closeModal);
  mask.addEventListener('click', function (e) { if (e.target === mask) closeModal(); });

  /* ============================================================
   * 玩家名输入（在线玩家下拉建议）
   * ============================================================ */
  function playerField() {
    var wrap = document.createElement('div');
    wrap.className = 'player-suggest';
    var input = document.createElement('input');
    input.type = 'text';
    input.placeholder = '输入玩家名（可从下方在线玩家选择）';
    var dd = document.createElement('div');
    dd.className = 'player-dropdown';
    state.onlinePlayers.forEach(function (p) {
      var d = document.createElement('div');
      d.textContent = p.name + (p.op ? ' [OP]' : '');
      d.addEventListener('click', function () { input.value = p.name; dd.classList.remove('show'); });
      dd.appendChild(d);
    });
    input.addEventListener('focus', function () { if (state.onlinePlayers.length) dd.classList.add('show'); });
    input.addEventListener('input', function () { dd.classList.remove('show'); });
    wrap.appendChild(input);
    wrap.appendChild(dd);
    return { wrap: wrap, value: function () { return input.value.trim(); } };
  }

  function refreshOnlinePlayers(then) {
    MC.api('/admin/players').then(function (r) {
      if (r.json && r.json.code === 200) state.onlinePlayers = r.json.data || [];
      if (then) then();
    });
  }

  /* ============================================================
   * 概览 + 礼包列表
   * ============================================================ */
  function loadTiers() {
    MC.api('/admin/overview').then(function (r) {
      if (r.json && r.json.code === 200) { state.overview = r.json.data; renderStats(); }
    });
    MC.api('/admin/tiers').then(function (r) {
      if (r.json && r.json.code === 200) {
        state.tiers = r.json.data || [];
        renderTiers();
      } else {
        MC.toast(accessMsg(r.json), 'err');
      }
    });
    refreshOnlinePlayers();
  }

  function accessMsg(json) {
    if (json && json.msg) return json.msg;
    return '请求失败：可能无权限或未以在线 OP 身份登录';
  }

  function renderStats() {
    var o = state.overview;
    if (!o) return;
    var cards = [
      ['c-cyan', '在线玩家', o.onlinePlayers, '人'],
      ['c-blue', '记录玩家', o.recordedPlayers, '人'],
      ['c-gold', '本月领取', o.claimedThisMonth, '次'],
      ['c-green', '礼包档位', o.tiersCount, '个 · ' + o.currentMonth]
    ];
    document.getElementById('stat-grid').innerHTML = cards.map(function (c) {
      return '<div class="stat-card ' + c[0] + '"><div class="lab">' + c[1] + '</div>'
        + '<div class="num">' + c[2] + ' <small>' + c[3] + '</small></div></div>';
    }).join('');
  }

  function renderTiers() {
    var tb = document.getElementById('tiers-tbody');
    tb.innerHTML = '';
    if (!state.tiers.length) {
      tb.innerHTML = '<tr><td colspan="7"><div class="empty-state">暂无礼包档位，请检查 rewards.yml</div></td></tr>';
      return;
    }
    state.tiers.forEach(function (t) {
      var tr = document.createElement('tr');

      var tdKey = document.createElement('td');
      tdKey.className = 'tier-key';
      tdKey.innerHTML = t.key + '<small>' + tierCN(t.key) + '</small>';

      var tdPerm = document.createElement('td');
      tdPerm.innerHTML = t.permission
        ? '<span class="mono" style="font-size:12px;">' + t.permission + '</span>'
        : '<span class="muted">无（所有人）</span>';

      var tdState = document.createElement('td');
      tdState.innerHTML = t.enabled
        ? '<span class="chip on">启用中</span>'
        : '<span class="chip off">已禁用</span>';

      var tdMoney = document.createElement('td');
      tdMoney.className = t.money > 0 ? '' : 'muted';
      tdMoney.textContent = t.money > 0 ? t.money : '—';

      var tdPoints = document.createElement('td');
      tdPoints.className = t.points > 0 ? '' : 'muted';
      tdPoints.textContent = t.points > 0 ? t.points : '—';

      var tdClaimed = document.createElement('td');
      tdClaimed.innerHTML = '<b>' + t.claimedThisMonth + '</b> <span class="muted">人领取</span>'
        + '<div class="muted" style="font-size:11.5px;margin-top:2px;">物品 ' + t.itemsCount + ' · 指令 ' + t.commandsCount + '</div>';

      var tdAct = document.createElement('td');
      tdAct.appendChild(rowActions(t));

      tr.appendChild(tdKey); tr.appendChild(tdPerm); tr.appendChild(tdState);
      tr.appendChild(tdMoney); tr.appendChild(tdPoints); tr.appendChild(tdClaimed); tr.appendChild(tdAct);
      tb.appendChild(tr);
    });
  }

  function tierCN(key) {
    return { default: '基础月卡', vip: 'VIP 月卡', mvp: 'MVP 月卡' }[key] || key;
  }

  /* 行内 6 个操作按钮 */
  function rowActions(t) {
    var box = document.createElement('div');
    box.className = 'row-actions';

    box.appendChild(ib('领取', 'green', icon('gift'), function () { claimDialog(t); }));
    box.appendChild(ib('重置领取', 'red', icon('reset'), function () { resetDialog(t); }));
    box.appendChild(ib('礼品配置', 'gold', icon('grid'), function () { giftsDialog(t); }));
    box.appendChild(ib('配置', '', icon('gear'), function () { configDialog(t); }));
    box.appendChild(ib('领取记录', '', icon('list'), function () { recordsDialog(t); }));

    var toggle = ib(t.enabled ? '禁用' : '启用',
      t.enabled ? 'red' : 'green',
      t.enabled ? icon('ban') : icon('check'),
      function () { doToggle(t, toggle); });
    box.appendChild(toggle);
    return box;
  }

  function ib(label, cls, svg, onClick) {
    var b = document.createElement('button');
    b.className = 'ib ' + (cls || '');
    b.innerHTML = svg + '<span>' + label + '</span>';
    b.addEventListener('click', onClick);
    return b;
  }
  function icon(name) {
    var P = {
      gift:'M20 12v9H4v-9M2 7h20v5H2zM12 22V7M12 7S9 2 6.5 3.5 9 7 12 7zM12 7s3-5 5.5-3.5S15 7 12 7z',
      reset:'M3 12a9 9 0 1 0 3-6.7M3 4v4h4',
      grid:'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z',
      gear:'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19 12a7 7 0 0 0-.1-1.2l2-1.6-2-3.4-2.4 1a7 7 0 0 0-2-1.2L14 3h-4l-.5 2.6a7 7 0 0 0-2 1.2l-2.4-1-2 3.4 2 1.6A7 7 0 0 0 5 12c0 .4 0 .8.1 1.2l-2 1.6 2 3.4 2.4-1a7 7 0 0 0 2 1.2L10 21h4l.5-2.6a7 7 0 0 0 2-1.2l2.4 1 2-3.4-2-1.6c.1-.4.1-.8.1-1.2z',
      list:'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
      ban:'M4 4l16 16M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z',
      check:'M20 6L9 17l-5-5'
    };
    return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="' + (P[name] || '') + '"/></svg>';
  }

  /* ============================================================
   * 启用 / 禁用
   * ============================================================ */
  function doToggle(t, btn) {
    MC.api('/admin/tiers/' + t.key + '/toggle', { method: 'POST' }).then(function (r) {
      if (r.json && r.json.code === 200) {
        MC.toast(r.json.msg || '已切换状态', 'ok');
        loadTiers();
      } else {
        MC.toast(accessMsg(r.json), 'err');
      }
    });
  }

  /* ============================================================
   * 领取（代在线玩家领取）
   * ============================================================ */
  function claimDialog(t) {
    refreshOnlinePlayers(function () {
      var pf = playerField();
      var tip = document.createElement('div');
      tip.className = 'field hint';
      tip.style.marginTop = '10px';
      tip.textContent = '代领要求目标玩家当前在线；领取动作在服务器主线程执行。';
      var box = document.createElement('div');
      box.appendChild(labeled('目标玩家', pf.wrap));
      box.appendChild(tip);
      openModal({
        title: '为玩家领取 · ' + tierCN(t.key),
        content: box,
        buttons: [
          { label: '取消' },
          { label: '确认领取', cls: 'primary', onClick: function (close) {
            var name = pf.value();
            if (!name) { MC.toast('请填写玩家名', 'err'); return; }
            MC.api('/admin/tiers/' + t.key + '/claim',
              { method: 'POST', body: { player: name } }).then(function (r) {
              if (r.json && r.json.code === 200) { MC.toast(r.json.msg, 'ok'); close(); loadTiers(); }
              else MC.toast(accessMsg(r.json), 'err');
            });
          }}
        ]
      });
    });
  }

  /* ============================================================
   * 重置领取
   * ============================================================ */
  function resetDialog(t) {
    refreshOnlinePlayers(function () {
      var pf = playerField();
      var tip = document.createElement('div');
      tip.className = 'field hint';
      tip.style.marginTop = '10px';
      tip.textContent = '重置后该玩家本月可再次领取；离线玩家也可重置（按正版/离线 UUID 匹配）。';
      var box = document.createElement('div');
      box.appendChild(labeled('目标玩家', pf.wrap));
      box.appendChild(tip);
      openModal({
        title: '重置玩家领取 · ' + tierCN(t.key),
        content: box,
        buttons: [
          { label: '取消' },
          { label: '确认重置', cls: 'danger', onClick: function (close) {
            var name = pf.value();
            if (!name) { MC.toast('请填写玩家名', 'err'); return; }
            MC.api('/admin/tiers/' + t.key + '/reset',
              { method: 'POST', body: { player: name } }).then(function (r) {
              if (r.json && r.json.code === 200) { MC.toast(r.json.msg, 'ok'); close(); loadTiers(); }
              else MC.toast(accessMsg(r.json), 'err');
            });
          }}
        ]
      });
    });
  }

  /* ============================================================
   * 领取记录
   * ============================================================ */
  function recordsDialog(t) {
    var box = document.createElement('div');
    box.innerHTML = '<div class="empty-state">加载中…</div>';
    openModal({
      title: '领取记录 · ' + tierCN(t.key), size: 'lg', content: box,
      buttons: [{ label: '关闭' }]
    });
    MC.api('/admin/tiers/' + t.key + '/records').then(function (r) {
      if (r.json && r.json.code === 200) {
        var d = r.json.data;
        if (!d.records || !d.records.length) {
          box.innerHTML = '<div class="empty-state">该档位暂无领取记录</div>';
          return;
        }
        var html = '<div class="muted" style="margin-bottom:10px;font-size:12.5px;">共 ' + d.total + ' 条记录（按月份排序）</div>'
          + '<div class="table-wrap"><table><colgroup><col style="width:28%"><col style="width:30%"><col style="width:42%"></colgroup>'
          + '<thead><tr><th>玩家</th><th>UUID</th><th>领取月份</th></tr></thead><tbody>';
        d.records.forEach(function (rec) {
          html += '<tr><td>' + MC.escapeHtml(rec.name || '未知') + '</td><td class="mono" style="font-size:11px;">'
            + rec.uuid + '</td><td class="mono">' + rec.month + '</td></tr>';
        });
        html += '</tbody></table></div>';
        box.innerHTML = html;
      } else {
        box.innerHTML = '<div class="empty-state">' + accessMsg(r.json) + '</div>';
      }
    });
  }

  function labeled(text, node) {
    var f = document.createElement('div');
    f.className = 'field';
    var l = document.createElement('label');
    l.textContent = text;
    f.appendChild(l);
    f.appendChild(node);
    return f;
  }

  /* ============================================================
   * 配置弹窗（权限 / 启用 / 金币 / 点券 / 指令；物品在礼品配置管理）
   * ============================================================ */
  function configDialog(t) {
    var box = document.createElement('div');
    box.innerHTML = '<div class="empty-state">加载配置中…</div>';
    openModal({
      title: '礼包配置 · ' + tierCN(t.key), size: 'lg', content: box,
      buttons: [{ label: '取消' }, { label: '保存配置', cls: 'primary', onClick: function (close) {
        var payload = {
          permission: box.querySelector('[data-f=permission]').value.trim(),
          enabled: box.querySelector('[data-f=enabled]').checked,
          money: parseInt(box.querySelector('[data-f=money]').value, 10) || 0,
          points: parseInt(box.querySelector('[data-f=points]').value, 10) || 0,
          commands: box.querySelector('[data-f=commands]').value.split('\n').map(function (s) { return s.trim(); }).filter(Boolean)
        };
        MC.api('/admin/tiers/' + t.key, { method: 'PUT', body: payload }).then(function (r) {
          if (r.json && r.json.code === 200) { MC.toast(r.json.msg, 'ok'); close(); loadTiers(); }
          else MC.toast(accessMsg(r.json), 'err');
        });
      }}]
    });

    MC.api('/admin/tiers/' + t.key).then(function (r) {
      if (r.json && r.json.code === 200) {
        var d = r.json.data, rw = d.rewards || {};
        box.innerHTML =
          '<div class="form-section"><h3>访问与状态</h3>'
          + fieldHtml('权限节点（留空表示所有玩家可领）', '<input type="text" data-f="permission" value="' + MC.escapeHtml(d.permission || '') + '">')
          + '<label class="switch"><input type="checkbox" data-f="enabled" ' + (d.enabled ? 'checked' : '') + '>启用该礼包档位（禁用后玩家无法领取，用户页也不展示）</label></div>'
          + '<div class="form-section"><h3>经济奖励</h3><div class="row2">'
          + fieldHtml('金币（Vault，0 表示不发放）', '<input type="number" min="0" data-f="money" value="' + (rw.money || 0) + '">')
          + fieldHtml('点券（PlayerPoints，0 表示不发放）', '<input type="number" min="0" data-f="points" value="' + (rw.points || 0) + '">')
          + '</div></div>'
          + '<div class="form-section"><h3>领取时执行指令</h3>'
          + fieldHtml('每行一条，无需以 / 开头；可用 %player% 占位', '<textarea data-f="commands">' + MC.escapeHtml((rw.commands || []).join('\n')) + '</textarea>')
          + '<div class="hint">物品奖励请点击列表中的【礼品配置】进行可视化编辑。</div></div>';
      } else {
        box.innerHTML = '<div class="empty-state">' + accessMsg(r.json) + '</div>';
      }
    });
  }

  function fieldHtml(label, inner) {
    return '<div class="field"><label>' + label + '</label>' + inner + '</div>';
  }

  /* ============================================================
   * 仿 mcmod 物品编辑器（独立二级弹窗）
   *  左：大图标预览（附魔流光）+ 实时 tooltip；右：属性表单
   * ============================================================ */
  function itemEditor(initial, onSave, onDelete) {
    var draft = {
      material: initial.material || 'DIAMOND',
      amount: initial.amount || 1,
      data: initial.data || 0,
      name: initial.name || '',
      lore: initial.lore ? initial.lore.slice() : [],
      enchants: initial.enchants ? initial.enchants.slice() : []
    };

    var sub = document.createElement('div');
    sub.className = 'modal-mask show';
    sub.style.zIndex = 130;
    var box = document.createElement('div');
    box.className = 'modal lg';
    sub.appendChild(box);

    /* head */
    var head = document.createElement('div');
    head.className = 'modal-head';
    head.innerHTML = '<h2>物品编辑器</h2>';
    var x = document.createElement('button');
    x.className = 'modal-close'; x.textContent = '×';
    x.addEventListener('click', close);
    head.appendChild(x);
    box.appendChild(head);

    /* body */
    var body = document.createElement('div');
    body.className = 'modal-body';
    body.innerHTML =
      '<div class="editor">'
      + '<div class="preview-stage">'
      +   '<div class="big-item" id="ed-big"><div id="ed-icon" style="width:96px;height:96px;"></div><div class="glint"></div></div>'
      +   '<div class="pv-tt" id="ed-tt"></div>'
      + '</div>'
      + '<div>'
      +   fieldHtml('物品材质 Material（1.12.2 名称，如 TOTEM、NETHER_STAR）',
            '<input type="text" id="ed-material" list="mat-list" value="' + MC.escapeHtml(draft.material) + '">'
            + '<datalist id="mat-list">' + COMMON_MATS.map(function (m) { return '<option value="' + m + '">'; }).join('') + '</datalist>')
      +   '<div class="row2">'
      +     fieldHtml('数量 Amount', '<input type="number" min="1" max="64" id="ed-amount" value="' + draft.amount + '">')
      +     fieldHtml('数据值/耐久 Data（一般为 0）', '<input type="number" min="0" id="ed-data" value="' + draft.data + '">')
      +   '</div>'
      +   fieldHtml('自定义名称（支持 & 颜色码，如 &bVIP 专属）', '<input type="text" id="ed-name" value="' + MC.escapeHtml(draft.name) + '">')
      +   fieldHtml('物品描述 Lore（每行一条，支持 & 颜色码）', '<textarea id="ed-lore">' + MC.escapeHtml(draft.lore.join('\n')) + '</textarea>')
      +   '<div class="form-section"><h3>附魔 Enchantments</h3><div class="ench-list" id="ed-ench-list"></div>'
      +     '<button class="btn" id="ed-add-ench" style="margin-top:10px;">添加附魔</button></div>'
      + '</div>'
      + '</div>';
    box.appendChild(body);

    /* foot */
    var foot = document.createElement('div');
    foot.className = 'modal-foot';
    if (onDelete) {
      var del = document.createElement('button');
      del.className = 'btn danger'; del.textContent = '删除此物品';
      del.style.marginRight = 'auto';
      del.addEventListener('click', function () { onDelete(); close(); });
      foot.appendChild(del);
    }
    var cancel = document.createElement('button');
    cancel.className = 'btn'; cancel.textContent = '取消';
    cancel.addEventListener('click', close);
    var save = document.createElement('button');
    save.className = 'btn gold'; save.textContent = '保存物品';
    save.addEventListener('click', function () {
      collect();
      if (!draft.material) { MC.toast('请填写物品材质', 'err'); return; }
      var item = { material: draft.material.toUpperCase() };
      if (parseInt(draft.amount, 10) > 1) item.amount = parseInt(draft.amount, 10);
      if (parseInt(draft.data, 10) > 0) item.data = parseInt(draft.data, 10);
      if (draft.name) item.name = draft.name;
      if (draft.lore.length) item.lore = draft.lore;
      if (draft.enchants.length) item.enchants = draft.enchants;
      onSave(item);
      close();
    });
    foot.appendChild(cancel); foot.appendChild(save);
    box.appendChild(foot);

    document.body.appendChild(sub);

    /* 附魔行渲染 */
    var enchList = body.querySelector('#ed-ench-list');
    function renderEnch() {
      enchList.innerHTML = '';
      if (!draft.enchants.length) {
        enchList.innerHTML = '<div class="muted" style="font-size:12.5px;">暂无附魔</div>';
        return;
      }
      draft.enchants.forEach(function (e, i) {
        var p = e.split(':');
        var row = document.createElement('div');
        row.className = 'ench-row';
        var sel = document.createElement('select');
        ENCHANTS.forEach(function (en) {
          var o = document.createElement('option');
          o.value = en[0]; o.textContent = en[1] + ' (' + en[0] + ')';
          if (en[0] === p[0]) o.selected = true;
          sel.appendChild(o);
        });
        var lvl = document.createElement('input');
        lvl.type = 'number'; lvl.min = '1'; lvl.max = '10'; lvl.value = p[1] || 1;
        var rm = document.createElement('button');
        rm.className = 'btn danger'; rm.textContent = '×';
        rm.addEventListener('click', function () { draft.enchants.splice(i, 1); renderEnch(); refresh(); });
        sel.addEventListener('change', function () { draft.enchants[i] = sel.value + ':' + lvl.value; refresh(); });
        lvl.addEventListener('input', function () { draft.enchants[i] = sel.value + ':' + lvl.value; refresh(); });
        row.appendChild(sel); row.appendChild(lvl); row.appendChild(rm);
        enchList.appendChild(row);
      });
    }
    body.querySelector('#ed-add-ench').addEventListener('click', function () {
      draft.enchants.push('DAMAGE_ALL:1');
      renderEnch(); refresh();
    });

    /* 从表单收集文本字段 */
    function collect() {
      draft.material = body.querySelector('#ed-material').value.trim();
      draft.amount = body.querySelector('#ed-amount').value;
      draft.data = body.querySelector('#ed-data').value;
      draft.name = body.querySelector('#ed-name').value;
      draft.lore = body.querySelector('#ed-lore').value.split('\n').map(function (s) { return s; }).filter(function (s, idx, arr) { return s !== '' || idx < arr.length; });
      draft.lore = body.querySelector('#ed-lore').value.split('\n');
    }

    /* 刷新预览 */
    function refresh() {
      var mat = draft.material.toUpperCase();
      var matrix = MC.ICONS[mat];
      var big = body.querySelector('#ed-big');
      body.querySelector('#ed-icon').innerHTML = matrix
        ? MC.iconSvg(matrix)
        : '<div style="font-family:Courier New;font-weight:800;font-size:34px;color:#888;">' + (draft.material.charAt(0) || '?') + '</div>';
      big.classList.toggle('enchanted', draft.enchants.length > 0);

      var name = draft.name || MC.prettyMaterial(draft.material);
      var tt = '<div style="color:' + (draft.name ? '#fff' : '#55ff55') + '">' + MC.colorToHtml(name) + '</div>';
      if (draft.lore && draft.lore.join('').length) {
        tt += draft.lore.map(MC.colorToHtml).join('<br>');
      }
      if (draft.enchants.length) {
        tt += draft.enchants.map(function (e) {
          var p = e.split(':');
          return '<div style="color:#bbb;">' + MC.prettyEnchant(p[0]) + ' ' + MC.roman(p[1] || 1) + '</div>';
        }).join('');
      }
      body.querySelector('#ed-tt').innerHTML = tt;
    }

    ['#ed-material', '#ed-amount', '#ed-data', '#ed-name', '#ed-lore'].forEach(function (sel) {
      body.querySelector(sel).addEventListener('input', function () { collect(); refresh(); });
    });

    renderEnch();
    refresh();

    function close() { sub.remove(); }
  }

  /* ============================================================
   * 礼品配置（MC 物品栏格子 + 背包拷贝）
   * ============================================================ */
  function giftsDialog(t) {
    var working = [];          // 正在编辑的物品数组
    var invWrap = document.createElement('div');

    /* 工具栏 */
    var tools = document.createElement('div');
    tools.className = 'inv-tools';
    var copyBtn = document.createElement('button');
    copyBtn.className = 'btn gold';
    copyBtn.innerHTML = icon('gift') + '读取我的背包并拷贝';
    var cap = document.createElement('span');
    cap.className = 'inv-cap sp';
    cap.textContent = '点击空格添加物品，点击已有物品进行编辑';
    tools.appendChild(copyBtn);
    tools.appendChild(cap);

    /* 背包拷贝区（默认隐藏） */
    var copyArea = document.createElement('div');
    copyArea.style.display = 'none';
    copyArea.style.marginBottom = '16px';

    /* 物品栏网格 */
    var grid = document.createElement('div');
    grid.className = 'inv-grid';

    invWrap.appendChild(tools);
    invWrap.appendChild(copyArea);
    invWrap.appendChild(grid);

    function renderGrid() {
      grid.innerHTML = '';
      var slots = 27;
      for (var i = 0; i < slots; i++) {
        (function (idx) {
          var s = document.createElement('div');
          s.className = 'inv-slot' + (working[idx] ? '' : ' empty');
          if (working[idx]) {
            var px = document.createElement('div');
            px.className = 'px';
            var mat = working[idx].material.toUpperCase();
            px.innerHTML = MC.ICONS[mat] ? MC.iconSvg(MC.ICONS[mat])
              : '<div class="ph">' + (working[idx].material.charAt(0) || '?') + '</div>';
            s.appendChild(px);
            var amt = parseInt(working[idx].amount, 10) || 1;
            if (amt > 1) {
              var a = document.createElement('span');
              a.className = 'amt'; a.textContent = amt;
              s.appendChild(a);
            }
            bindSlotTooltip(s, working[idx]);
            s.addEventListener('click', function () {
              itemEditor(working[idx], function (item) { working[idx] = item; renderGrid(); },
                function () { working.splice(idx, 1); renderGrid(); });
            });
          } else {
            s.addEventListener('click', function () {
              itemEditor({ material: 'DIAMOND', amount: 1 }, function (item) {
                var firstEmpty = working.length;
                working[firstEmpty] = item;
                renderGrid();
              });
            });
          }
          grid.appendChild(s);
        })(i);
      }
    }

    function bindSlotTooltip(el, item) {
      el.addEventListener('mouseenter', function () {
        var tt = document.getElementById('tooltip');
        var nameColor = item.name ? '#ffffff' : '#55ff55';
        var html = '<div class="tt-name" style="color:' + nameColor + '">' + MC.colorToHtml(item.name || MC.prettyMaterial(item.material)) + '</div>';
        if (item.lore && item.lore.length) html += '<div class="tt-lore">' + item.lore.map(MC.colorToHtml).join('<br>') + '</div>';
        if (item.enchants && item.enchants.length) html += '<div class="tt-extra">' + item.enchants.map(function (e) {
          var p = e.split(':'); return MC.prettyEnchant(p[0]) + ' ' + MC.roman(p[1] || 1);
        }).join('<br>') + '</div>';
        tt.innerHTML = html; tt.style.display = 'block';
        moveTT(el);
        el.addEventListener('mousemove', function (e) {
          tt.style.left = (e.clientX + 14) + 'px'; tt.style.top = (e.clientY + 12) + 'px';
        });
      });
      el.addEventListener('mouseleave', function () { document.getElementById('tooltip').style.display = 'none'; });
    }
    function moveTT(el) {
      var tt = document.getElementById('tooltip');
      var r = el.getBoundingClientRect();
      tt.style.left = (r.right + 6) + 'px'; tt.style.top = r.top + 'px';
    }

    /* 读取管理员背包 */
    copyBtn.addEventListener('click', function () {
      copyBtn.disabled = true;
      copyArea.innerHTML = '<div class="empty-state">正在读取背包（需管理员在线）…</div>';
      copyArea.style.display = 'block';
      MC.api('/admin/inventory').then(function (r) {
        copyBtn.disabled = false;
        if (r.json && r.json.code === 200) {
          var d = r.json.data;
          renderCopyArea(d);
        } else {
          copyArea.innerHTML = '<div class="empty-state">' + accessMsg(r.json) + '</div>';
        }
      });
    });

    function renderCopyArea(d) {
      copyArea.innerHTML = '';
      var title = document.createElement('div');
      title.className = 'inv-cap';
      title.style.marginBottom = '8px';
      title.innerHTML = '背包玩家：<b style="color:var(--cyan);">' + MC.escapeHtml(d.operator) + '</b> · 点击物品追加到礼品列表';
      var cg = document.createElement('div');
      cg.className = 'inv-grid';
      (d.items || []).forEach(function (it) {
        var s = document.createElement('div');
        s.className = 'inv-slot' + (it ? '' : ' empty');
        if (it) {
          var px = document.createElement('div'); px.className = 'px';
          px.innerHTML = MC.ICONS[it.material] ? MC.iconSvg(MC.ICONS[it.material])
            : '<div class="ph">' + (it.material.charAt(0) || '?') + '</div>';
          s.appendChild(px);
          var amt = parseInt(it.amount, 10) || 1;
          if (amt > 1) { var a = document.createElement('span'); a.className = 'amt'; a.textContent = amt; s.appendChild(a); }
          s.addEventListener('click', function () {
            working.push(cloneItem(it));
            renderGrid();
            MC.toast('已追加 ' + (it.name || it.material), 'ok');
          });
        }
        cg.appendChild(s);
      });
      copyArea.appendChild(title);
      copyArea.appendChild(cg);
    }

    openModal({
      title: '礼品配置 · ' + tierCN(t.key), size: 'lg', content: invWrap,
      buttons: [
        { label: '取消' },
        { label: '保存礼品配置', cls: 'gold', onClick: function (close) {
          var items = working.filter(Boolean);
          MC.api('/admin/tiers/' + t.key, { method: 'PUT', body: { items: items } }).then(function (r) {
            if (r.json && r.json.code === 200) { MC.toast(r.json.msg, 'ok'); close(); loadTiers(); }
            else MC.toast(accessMsg(r.json), 'err');
          });
        }}
      ]
    });

    /* 载入当前物品 */
    MC.api('/admin/tiers/' + t.key).then(function (r) {
      if (r.json && r.json.code === 200) {
        working = ((r.json.data.rewards || {}).items || []).map(cloneItem);
        renderGrid();
      }
    });
  }

  function cloneItem(it) {
    var c = { material: it.material };
    ['amount', 'data'].forEach(function (k) { if (it[k] != null) c[k] = it[k]; });
    ['name'].forEach(function (k) { if (it[k]) c[k] = it[k]; });
    ['lore', 'enchants'].forEach(function (k) { if (it[k] && it[k].length) c[k] = it[k].slice(); });
    return c;
  }


  /* ============================================================
   * 设置：加载 / 渲染 / 保存
   * ============================================================ */
  function loadSettings() {
    var body = document.getElementById('settings-body');
    body.innerHTML = '<div class="empty-state">加载中…</div>';
    MC.api('/admin/settings').then(function (r) {
      if (r.json && r.json.code === 200) {
        state.settings = r.json.data;
        renderSettings(body);
      } else {
        body.innerHTML = '<div class="empty-state">' + accessMsg(r.json) + '</div>';
      }
    });
  }

  function renderSettings(container) {
    var d = state.settings, s = d.settings || {}, be = d.backends || {}, mi = d.mirror || {};
    container.innerHTML = '';

    /* 基础设置 */
    var base = document.createElement('div');
    base.className = 'form-section';
    base.innerHTML = '<h3>基础设置</h3>'
      + '<div class="row2">'
      +   fieldHtml('每月领取日（1-28）', '<input type="number" min="1" max="28" data-k="claim-day" value="' + (s['claim-day'] != null ? s['claim-day'] : 1) + '">')
      +   fieldHtml('月份格式（Java 日期模式）', '<input type="text" data-k="month-format" value="' + MC.escapeHtml(s['month-format'] || 'yyyy-MM') + '">')
      + '</div>'
      + '<div class="row2">'
      +   fieldHtml('金币单位文案', '<input type="text" data-k="units.money" value="' + MC.escapeHtml(s['units.money'] || '金币') + '">')
      +   fieldHtml('点券单位文案', '<input type="text" data-k="units.points" value="' + MC.escapeHtml(s['units.points'] || '点券') + '">')
      + '</div>'
      + switchHtml('auto-claim-on-join', '玩家上线时自动尝试领取', s['auto-claim-on-join'])
      + switchHtml('offline-compensation', '离线补发（可领期间离线，上线自动补发）', s['offline-compensation'])
      + switchHtml('debug', '调试模式（输出存储读写细节）', s['debug']);
    container.appendChild(base);

    /* 审计 */
    var audit = document.createElement('div');
    audit.className = 'form-section';
    audit.innerHTML = '<h3>领取审计</h3>'
      + switchHtml('audit.enabled', '启用领取审计日志', s['audit.enabled'])
      + '<div class="hint">日志文件路径（在插件数据目录下，暂不支持网页修改）：' + MC.escapeHtml(s['audit.file'] || 'logs/claims.log') + '</div>';
    container.appendChild(audit);

    /* 存储后端 */
    var storage = document.createElement('div');
    storage.className = 'form-section';
    var beHtml = '<h3>存储后端（主辅模型）</h3>';
    var beNames = { yaml: 'YAML 文件', sqlite: 'SQLite 数据库', mysql: 'MySQL 数据库' };
    Object.keys(beNames).forEach(function (id) {
      var on = be[id] ? be[id].enabled : false;
      beHtml += switchHtml('be-' + id, beNames[id] + '（' + id + '）', on);
    });
    beHtml += '<div class="hint">优先级 MySQL &gt; SQLite &gt; YAML；启用的最高优先级后端为主存储。<b style="color:var(--amber);">后端启用变更需重启服务器生效</b>，连接串/账号请在 config.yml 中配置（网页不处理密码）。</div>';
    storage.innerHTML = beHtml;
    container.appendChild(storage);

    /* 镜像 */
    var mirror = document.createElement('div');
    mirror.className = 'form-section';
    mirror.innerHTML = '<h3>镜像策略（辅助存储热备份）</h3>'
      + switchHtml('mir.enabled', '写入时镜像到辅助存储', mi.enabled)
      + switchHtml('mir.async', '镜像写入异步执行（推荐）', mi.async)
      + switchHtml('mir.sync-on-startup', '启动时主存储全量同步到辅助存储', mi['sync-on-startup']);
    container.appendChild(mirror);

    /* 保存按钮 */
    var saveBar = document.createElement('div');
    saveBar.style.cssText = 'display:flex;justify-content:flex-end;gap:10px;margin-top:6px;';
    var reset = document.createElement('button');
    reset.className = 'btn'; reset.textContent = '放弃修改';
    reset.addEventListener('click', function () { loadSettings(); });
    var save = document.createElement('button');
    save.className = 'btn primary'; save.textContent = '保存设置';
    save.addEventListener('click', function () { saveSettings(container); });
    saveBar.appendChild(reset); saveBar.appendChild(save);
    container.appendChild(saveBar);
  }

  function switchHtml(k, label, checked) {
    return '<label class="switch" style="margin-bottom:11px;"><input type="checkbox" data-k="' + k + '" '
      + (checked ? 'checked' : '') + '><span>' + label + '</span></label>';
  }

  function saveSettings(container) {
    function val(k) {
      var el = container.querySelector('[data-k="' + k + '"]');
      if (!el) return null;
      if (el.type === 'checkbox') return el.checked;
      if (el.type === 'number') return parseInt(el.value, 10) || 0;
      return el.value;
    }
    var payload = {
      settings: {
        'claim-day': val('claim-day'),
        'month-format': val('month-format'),
        'auto-claim-on-join': val('auto-claim-on-join'),
        'offline-compensation': val('offline-compensation'),
        'debug': val('debug'),
        'audit.enabled': val('audit.enabled'),
        units: { money: val('units.money'), points: val('units.points') }
      },
      backends: {
        yaml: { enabled: val('be-yaml') },
        sqlite: { enabled: val('be-sqlite') },
        mysql: { enabled: val('be-mysql') }
      },
      mirror: {
        enabled: val('mir.enabled'),
        async: val('mir.async'),
        'sync-on-startup': val('mir.sync-on-startup')
      }
    };
    MC.api('/admin/settings', { method: 'PUT', body: payload }).then(function (r) {
      if (r.json && r.json.code === 200) { MC.toast(r.json.msg || '设置已保存', 'ok'); loadSettings(); }
      else MC.toast(accessMsg(r.json), 'err');
    });
  }

  /* ============================================================
   * 日志
   * ============================================================ */
  function loadLogs() {
    var box = document.getElementById('log-box');
    box.innerHTML = '<div class="empty-state">加载中…</div>';
    MC.api('/admin/logs').then(function (r) {
      if (r.json && r.json.code === 200) {
        var d = r.json.data;
        var st = document.getElementById('log-status');
        st.textContent = d.enabled ? '审计开启' : '审计关闭';
        st.className = 'chip ' + (d.enabled ? 'on' : 'off');
        document.getElementById('log-file').textContent = '文件：' + d.file + '（插件数据目录下，显示末尾 500 行）';
        if (!d.lines || !d.lines.length) {
          box.innerHTML = '<div class="empty-state">暂无日志内容</div>';
          return;
        }
        box.innerHTML = d.lines.map(function (line) {
          return '<div class="log-line">' + colorizeLog(line) + '</div>';
        }).join('');
        box.scrollTop = box.scrollHeight;
      } else {
        box.innerHTML = '<div class="empty-state">' + accessMsg(r.json) + '</div>';
      }
    });
  }

  function colorizeLog(line) {
    var esc = MC.escapeHtml(line);
    if (/失败|错误|异常|error|denied/i.test(line)) return '<span class="lvl-err">' + esc + '</span>';
    if (/成功|领取|claim|补发/i.test(line)) return '<span class="lvl-ok">' + esc + '</span>';
    return esc;
  }


  /* ============================================================
   * 启动
   * ============================================================ */
  document.getElementById('op-name').textContent = '需在线 OP';
  loadTiers();
})();
