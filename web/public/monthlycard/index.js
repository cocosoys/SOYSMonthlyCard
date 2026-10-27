/* =====================================================================
 * 用户侧 index.html 逻辑：Hypixel 风格月卡展示 + 下滑电梯
 * ===================================================================== */
(function () {
  'use strict';
  var MC = window.MC;

  /* 档位展示元信息：图标 / 中文名 / 英文 / 主题 class（未知档位用兜底） */
  var TIER_META = {
    default: { icon: 'DIAMOND',      cn: '基础月卡', en: 'DEFAULT', cls: 'tier-default' },
    vip:     { icon: 'NETHER_STAR',  cn: 'VIP 月卡', en: 'VIP',     cls: 'tier-vip' },
    mvp:     { icon: 'TOTEM',        cn: 'MVP 月卡', en: 'MVP',     cls: 'tier-mvp' }
  };
  function meta(key) {
    return TIER_META[key] || {
      icon: 'CHEST', cn: key.toUpperCase() + ' 月卡', en: key.toUpperCase(), cls: 'tier-default'
    };
  }

  var viewData = null;   // /view/tiers
  var stateData = null;  // /player/state

  /* ---------------- 渲染 Hero 信息 ---------------- */
  function renderHero() {
    if (!viewData) return;
    setText('pill-month', viewData.month || '—');
    setText('pill-day', viewData.claimDay != null ? viewData.claimDay : '—');
    var dot = document.getElementById('pill-dot');
    var time = document.getElementById('pill-time');
    if (viewData.claimTime) {
      dot.className = 'dot on';
      time.textContent = '领取开放中';
    } else {
      dot.className = 'dot';
      time.textContent = '未到领取时间';
    }
  }

  /* ---------------- 渲染楼层 ---------------- */
  function renderFloors() {
    var list = document.getElementById('floor-list');
    list.innerHTML = '';
    var tiers = (viewData && viewData.tiers) || [];

    tiers.forEach(function (tier, idx) {
      var m = meta(tier.key);
      var st = stateData && stateData.tiers ? stateData.tiers[tier.key] : null;

      var section = document.createElement('section');
      section.className = 'floor ' + m.cls;
      section.id = 'floor-' + tier.key;

      /* --- 档位徽章侧 --- */
      var badge = document.createElement('div');
      badge.className = 'tier-badge';
      var ticon = document.createElement('div');
      ticon.className = 'ticon';
      ticon.innerHTML = MC.iconSvg(MC.ICONS[m.icon] || MC.ICONS.CHEST);
      var tname = document.createElement('div');
      tname.className = 'tname';
      tname.textContent = m.en;
      var teng = document.createElement('div');
      teng.className = 'teng';
      teng.textContent = 'MONTHLY CARD · ' + (idx + 1);
      badge.appendChild(ticon);
      badge.appendChild(tname);
      badge.appendChild(teng);

      /* --- 内容侧 --- */
      var body = document.createElement('div');
      body.className = 'tier-body';

      var h3 = document.createElement('h3');
      h3.appendChild(document.createTextNode(m.cn + '礼包'));
      var tag = document.createElement('span');
      tag.className = 'tag';
      tag.textContent = statusText(st).label;
      h3.appendChild(tag);
      body.appendChild(h3);

      /* 金币 / 点券 */
      var rg = document.createElement('div');
      rg.className = 'reward-grid';
      if (tier.money > 0) rg.appendChild(currency('gold', tier.money, '金币'));
      if (tier.points > 0) rg.appendChild(currency('points', tier.points, '点券'));
      body.appendChild(rg);

      /* 物品槽位 */
      if (tier.items && tier.items.length) {
        var itemsRow = document.createElement('div');
        itemsRow.className = 'items-row';
        tier.items.forEach(function (it) {
          itemsRow.appendChild(MC.renderSlot(it));
        });
        body.appendChild(itemsRow);
      }

      /* 额外指令 */
      if (tier.commands && tier.commands.length) {
        var cmd = document.createElement('div');
        cmd.style.cssText = 'font-size:12.5px;color:var(--mut);margin-top:4px;';
        cmd.textContent = '额外执行 ' + tier.commands.length + ' 条专属指令';
        body.appendChild(cmd);
      }

      /* 底部状态 + 按钮 */
      var foot = document.createElement('div');
      foot.className = 'tier-foot';
      var note = document.createElement('div');
      note.className = 'status-note' + (st && st.claimed ? ' claimed' : '');
      note.innerHTML = statusText(st).note;
      var btn = document.createElement('button');
      btn.className = 'btn-claim';
      applyButton(btn, st);
      btn.addEventListener('click', function () {
        MC.toast('请进入游戏，使用 /monthlycard claim 领取本月礼包', 'ok');
      });
      foot.appendChild(note);
      foot.appendChild(btn);
      body.appendChild(foot);

      var card = document.createElement('div');
      card.className = 'tier-card';
      card.appendChild(badge);
      card.appendChild(body);
      section.appendChild(card);
      list.appendChild(section);
    });

    buildElevator(tiers);
  }

  function currency(kind, amount, label) {
    var el = document.createElement('div');
    el.className = 'currency ' + kind;
    var ci = document.createElement('div');
    ci.className = 'ci';
    ci.innerHTML = kind === 'gold'
      ? MC.iconSvg(MC.ICONS.GOLD_INGOT)
      : MC.iconSvg(MC.ICONS.EXPERIENCE_BOTTLE);
    var txt = document.createElement('span');
    txt.innerHTML = '<b>' + amount + '</b> ' + label;
    el.appendChild(ci);
    el.appendChild(txt);
    return el;
  }

  /* 根据领取状态返回文案 */
  function statusText(st) {
    if (!stateData) {
      return { label: '待登录', note: '登录网页账号后查看你的领取状态' };
    }
    if (!st) return { label: '—', note: '暂无状态信息' };
    if (st.claimed) {
      return { label: '已领取', note: '本月礼包已领取，下月刷新' };
    }
    if (!st.permitted) {
      return { label: '未解锁', note: '该档位需要对应权限 / 身份，升级后解锁' };
    }
    if (viewData && !viewData.claimTime) {
      return { label: '未开放', note: '尚未到每月领取开放日，请耐心等待' };
    }
    return { label: '可领取', note: '本月礼包待领取，请进入游戏领取' };
  }

  function applyButton(btn, st) {
    if (!stateData) { btn.disabled = true; btn.textContent = '登录后查看'; return; }
    if (!st) { btn.disabled = true; btn.textContent = '暂无状态'; return; }
    if (st.claimed) { btn.disabled = true; btn.textContent = '本月已领取'; return; }
    if (!st.permitted) { btn.disabled = true; btn.textContent = '未解锁该档位'; return; }
    if (viewData && !viewData.claimTime) { btn.disabled = true; btn.textContent = '未到领取时间'; return; }
    btn.disabled = false;
    btn.textContent = '前往游戏内领取';
  }

  /* ---------------- 电梯侧边导航 ---------------- */
  function buildElevator(tiers) {
    var ev = document.getElementById('elevator');
    ev.innerHTML = '<span class="rail"></span>';
    if (!tiers.length) { ev.style.display = 'none'; return; }
    ev.style.display = 'flex';

    tiers.forEach(function (tier) {
      var m = meta(tier.key);
      var b = document.createElement('button');
      b.dataset.target = 'floor-' + tier.key;
      var s = document.createElement('span');
      s.textContent = m.cn;
      b.appendChild(s);
      b.addEventListener('click', function () {
        var el = document.getElementById(b.dataset.target);
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
      ev.appendChild(b);
    });
    observeFloors();
  }

  function observeFloors() {
    var btns = document.querySelectorAll('#elevator button');
    function setActive(id) {
      btns.forEach(function (b) { b.classList.toggle('active', b.dataset.target === id); });
    }
    if ('IntersectionObserver' in window) {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (en) {
          if (en.isIntersecting) setActive(en.target.id);
        });
      }, { rootMargin: '-45% 0px -45% 0px', threshold: 0 });
      document.querySelectorAll('.floor').forEach(function (f) { io.observe(f); });
    } else {
      window.addEventListener('scroll', function () {
        var cur = null;
        document.querySelectorAll('.floor').forEach(function (f) {
          var r = f.getBoundingClientRect();
          if (r.top <= window.innerHeight * 0.5) cur = f.id;
        });
        if (cur) setActive(cur);
      });
    }
  }

  /* ---------------- 工具 ---------------- */
  function setText(id, v) { var el = document.getElementById(id); if (el) el.textContent = v; }

  /* ---------------- Mock 自检（仅 URL 带 ?mock=1 时触发，数据取自 rewards.yml 默认配置） ---------------- */
  var MOCK_VIEW = {
    month: '2026-09', claimDay: 1, claimTime: true,
    tiers: [
      { key: 'default', money: 1000, points: 100, commands: ['say %player_name% 领取了基础月卡'],
        items: [{ material: 'DIAMOND', amount: 1 }] },
      { key: 'vip', money: 5000, points: 500, commands: [],
        items: [
          { material: 'NETHER_STAR', amount: 1, name: '&bVIP专属星', lore: ['&7每月尊享福利'] },
          { material: 'DIAMOND', amount: 5 }
        ] },
      { key: 'mvp', money: 0, points: 2000, commands: [],
        items: [
          { material: 'TOTEM', amount: 1, name: '&6MVP专属图腾', lore: ['&e至臻月卡奖励'] },
          { material: 'NETHER_STAR', amount: 3, enchants: ['DAMAGE_ALL:1'] }
        ] }
    ]
  };
  var MOCK_STATE = {
    name: 'SOYS_Player',
    tiers: {
      default: { claimed: true, permitted: true },
      vip: { claimed: false, permitted: true },
      mvp: { claimed: false, permitted: false }
    }
  };
  function runMock() {
    viewData = MOCK_VIEW;
    stateData = MOCK_STATE;
    renderHero();
    renderFloors();
  }

  /* ---------------- 启动 ---------------- */
  function load() {
    if (window.__MOCK || /mock=1/.test(location.search + location.hash)) { runMock(); return; }
    MC.api('/view/tiers').then(function (r) {
      if (r.json && r.json.code === 200) {
        viewData = r.json.data;
        renderHero();
        return MC.api('/player/state');
      }
      return null;
    }).then(function (r) {
      if (r && r.json && r.json.code === 200) {
        stateData = r.json.data;
      }
    }).catch(function () {
      /* 网络或凭证异常：保留静态结构与提示 */
    }).then(function () {
      renderFloors();
    });
  }
  load();
})();
