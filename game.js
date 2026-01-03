(function(){
  const container = document.getElementById('gameContainer');
  const scoreEl = document.getElementById('score');
  const bestEl = document.getElementById('bestValue');
  const menu = document.getElementById('menu');
  const playBtn = document.getElementById('playBtn');
  const howBtn = document.getElementById('howBtn');
  const openAuth = document.getElementById('openAuth');
  const authModal = document.getElementById('authModal');
  const authUser = document.getElementById('authUser');
  const authPass = document.getElementById('authPass');
  const rememberMe = document.getElementById('rememberMe');
  const registerBtn = document.getElementById('registerBtn');
  const loginBtn = document.getElementById('loginBtn');
  const guestBtn = document.getElementById('guestBtn');
  const closeAuth = document.getElementById('closeAuth');
  const menuSignBtn = document.getElementById('menuSignBtn');
  const menuRegisterBtn = document.getElementById('menuRegisterBtn');
  const menuGuestBtn = document.getElementById('menuGuestBtn');
  const userBox = document.getElementById('userBox');
  const leadersList = document.getElementById('leaders');
  const audioToggle = document.getElementById('audioToggle');
  const audioVol = document.getElementById('audioVol');
  const christmasToggle = document.getElementById('christmasToggle');

  let scene, camera, renderer, playerMesh, obstacles = [], clock, speed, spawnTimer, score, running, best;
let menuPreview = { renderer: null, scene: null, camera: null, mesh: null, animId: null };
  let textures = [];
  let loader = null;
  let bullets = [];
  const bulletSpeed = 120; 
  const bulletDmg = 1;
  
  const speedoMin = 6;
  const speedoMax = 36;
  const lanes = [-2.4, 0, 2.4];
  let currentLane = 1; 
  let targetX = lanes[currentLane];
  
  let users = {}; 
  let currentUser = null;
  let guestCounter = 1; // for guestAuto()
  let specialAuto = false;
  let specialLastChange = 0;
  const specialMinChangeInterval = 0.18; 
  const specialLookaheadZ = 260; 
  
  let audioCtx = null;
  let musicGain = null;
  let musicFilter = null;
  let musicOsc1 = null;
  let musicOsc2 = null;
  let musicStarted = false;
  let musicMuted = false;
  let musicVolume = 0.6;
  // audio element for play SFX
  let playSfx = null; 
  
  // Jump / vertical physics
  let playerVy = 0;
  const GRAVITY = -30; // tuned gravity
  const JUMP_SPEED = 8.5;
  const GROUND_Y = 0.7;
  const JUMP_CLEAR_Y = GROUND_Y + 0.6;
  
  let padGain = null;
  let musicLFO = null;
  let lfoGain = null;
  let musicBeatAcc = 0;
  let musicBeatInterval = 0.45; 
  const baseFreq1 = 110;
  const baseFreq2 = 220;
  
  let snowPoints = null;
  let snowVel = null;
  const SNOW_COUNT = 420;

  // Settings persistence
  // Known zombie types we ship with (key, file, label)
  const ZOMBIE_TYPES = [
    { key: 'plantzombie', path: 'zm/plantzombie.png', label: 'Plant Zombie' },
    { key: 'stevezombie', path: 'zm/stevezombie.png', label: 'Steve Zombie' },
    { key: 'tralalelozombie', path: 'zm/tralalelozombie.jpg', label: 'Tralale Zombie' },
    { key: 'sahurzombie', path: 'zm/sahurzombie.jpg', label: 'Sahur Zombie' }
  ];
  // mapping of key -> texture
  let texturesByKey = {};

  let SETTINGS = {
    fov: 60,
    fog: 0.02,
    viewDistance: 1000,
    christmas: false,
    gfx: 'medium',
    res: 100,
    // default: enable all zombie types
    zombieAllow: (function(){ const m={}; ZOMBIE_TYPES.forEach(z=>m[z.key]=true); return m; })()
  };
  function loadSettings(){ try{ const raw = localStorage.getItem('runner3d_settings'); if(raw){ const s = JSON.parse(raw); SETTINGS = Object.assign(SETTINGS, s); // ensure defaults for zombieAllow
      if(!SETTINGS.zombieAllow){ SETTINGS.zombieAllow = {}; ZOMBIE_TYPES.forEach(z=>{ SETTINGS.zombieAllow[z.key] = true; }); }
    } }catch(e){} }
  function saveSettings(){ try{ localStorage.setItem('runner3d_settings', JSON.stringify(SETTINGS)); }catch(e){}
    populateSettingsUI(); }
  function populateSettingsUI(){ const fov = document.getElementById('fovRange'); const fovV = document.getElementById('fovValue'); const fog = document.getElementById('fogRange'); const fogV = document.getElementById('fogValue'); const view = document.getElementById('viewRange'); const viewV = document.getElementById('viewValue'); const ch = document.getElementById('christmasChk'); const gfx = document.getElementById('gfxSelect'); const res = document.getElementById('resRange'); const resV = document.getElementById('resValue'); const aVol = document.getElementById('audioVol'); const aVolLabel = document.getElementById('audioVolValue'); if(fov){ fov.value = SETTINGS.fov; if(fovV) fovV.textContent = SETTINGS.fov; } if(fog){ fog.value = SETTINGS.fog; if(fogV) fogV.textContent = SETTINGS.fog; } if(view){ view.value = SETTINGS.viewDistance; if(viewV) viewV.textContent = SETTINGS.viewDistance; } if(ch) ch.checked = !!SETTINGS.christmas; if(gfx) gfx.value = SETTINGS.gfx; if(res){ res.value = SETTINGS.res; if(resV) resV.textContent = SETTINGS.res; } if(aVol){ aVol.value = String(musicVolume); if(aVolLabel) aVolLabel.textContent = Math.round(musicVolume * 100); }
    // populate zombie type checkboxes (dynamic)
    try{
      const zCont = document.getElementById('zombieTypeList');
      if(zCont){ zCont.innerHTML = ''; ZOMBIE_TYPES.forEach(z=>{
        const id = 'zombieChk_' + z.key;
        const lbl = document.createElement('label'); lbl.style.display = 'inline-flex'; lbl.style.alignItems = 'center'; lbl.style.gap = '6px'; lbl.style.padding = '4px 8px'; lbl.style.borderRadius = '6px'; lbl.style.background = 'rgba(255,255,255,0.02)'; lbl.style.cursor = 'pointer';
        const cb = document.createElement('input'); cb.type = 'checkbox'; cb.id = id; cb.checked = !!(SETTINGS.zombieAllow && SETTINGS.zombieAllow[z.key]);
        cb.addEventListener('change', ()=>{ if(!SETTINGS.zombieAllow) SETTINGS.zombieAllow = {}; SETTINGS.zombieAllow[z.key] = cb.checked; });
        const sp = document.createElement('span'); sp.textContent = z.label;
        lbl.appendChild(cb); lbl.appendChild(sp); zCont.appendChild(lbl);
      }); }
    }catch(e){ }
    
    // update labels with translations if available
    const fovLabelEl = document.querySelector('label[for="fovRange"]'); if(fovLabelEl) { const sp = document.getElementById('fovValue'); fovLabelEl.childNodes[0].nodeValue = (t('fov_label') || 'Field of view') + ': '; if(sp) sp.textContent = SETTINGS.fov; }
    const fogLabelEl = document.querySelector('label[for="fogRange"]'); if(fogLabelEl){ const sp2 = document.getElementById('fogValue'); fogLabelEl.childNodes[0].nodeValue = (t('fog_label') || 'Fog density') + ': '; if(sp2) sp2.textContent = SETTINGS.fog; }
    const viewLabelEl = document.querySelector('label[for="viewRange"]'); if(viewLabelEl){ const sp4 = document.getElementById('viewValue'); viewLabelEl.childNodes[0].nodeValue = (t('view_label') || 'View distance') + ': '; if(sp4) sp4.textContent = SETTINGS.viewDistance; }
    const gfxLabelEl = document.querySelector('label[for="gfxSelect"]'); if(gfxLabelEl) gfxLabelEl.childNodes[0].nodeValue = (t('graphics') || 'Graphics') + ': ';
    const resLabelEl = document.querySelector('label[for="resRange"]'); if(resLabelEl){ const sp3 = document.getElementById('resValue'); resLabelEl.childNodes[0].nodeValue = (t('resolution') || 'Resolution') + ': '; if(sp3) sp3.textContent = SETTINGS.res; }
    // update audio UI text
    try{ updateAudioUI(); }catch(e){}
  }
  function updateRendererScale(){ if(!renderer) return; const device = window.devicePixelRatio || 1; let scale = (SETTINGS.res || 100)/100; const gfxMul = (SETTINGS.gfx === 'low') ? 0.8 : (SETTINGS.gfx === 'high') ? 1.25 : 1.0; const targetPR = Math.max(0.5, Math.min(2.0, device * scale * gfxMul)); renderer.setPixelRatio(Math.min(targetPR, window.devicePixelRatio || 2)); renderer.setSize(container.clientWidth, container.clientHeight); }
  function applySettings(){ try{ if(camera){ camera.fov = Number(SETTINGS.fov) || 60; camera.far = Number(SETTINGS.viewDistance) || 1000; camera.updateProjectionMatrix(); }
    if(scene && scene.fog){ scene.fog.density = Number(SETTINGS.fog) || 0.02; }
    // also apply to the menu preview if present so the lobby reflects changes
    try{
      if(menuPreview && menuPreview.camera){ menuPreview.camera.fov = Math.max(40, Math.min(100, Number(SETTINGS.fov) || 60) * 0.75); menuPreview.camera.far = Math.max(50, (Number(SETTINGS.viewDistance) || 1000) * 0.6); menuPreview.camera.updateProjectionMatrix(); }
      if(menuPreview && menuPreview.scene){ menuPreview.scene.fog = new THREE.FogExp2(0x071022, Number(SETTINGS.fog) || 0.02); }
    }catch(e){}
    if(SETTINGS.christmas){ startChristmas(); } else { stopChristmas(); }
    updateRendererScale(); populateSettingsUI(); }catch(e){ console.warn('applySettings failed', e); } }

  
  // --- i18n: supported languages and translations (minimal set) ---
  const LANGS = [
    {code:'en', label:'English'},
    {code:'zh', label:'中文'},
    {code:'es', label:'Español'},
    {code:'ar', label:'العربية'},
    {code:'hi', label:'हिन्दी'},
    {code:'bn', label:'বাংলা'},
    {code:'pt', label:'Português'},
    {code:'ru', label:'Русский'},
    {code:'ja', label:'日本語'},
    {code:'de', label:'Deutsch'}
  ];
  let currentLang = localStorage.getItem('runner3d_lang') || (navigator.language ? navigator.language.slice(0,2) : 'en');
  const TRANSLATIONS = {
    en: { menu_default: 'Use Left/Right arrows or touch left/right halves to change lanes. Avoid obstacles and survive as long as you can.', menu_welcome: 'Welcome, {name}', play: 'Play', how: 'How to', sign_in: 'Sign in', register: 'Register', continue_guest: 'Continue as Guest', account: 'Account', username_placeholder: 'Username', password_placeholder: 'Password', remember: 'Remember me (store locally)', controls_text: 'Controls: Use Left/Right arrows (or touch halves) to change lanes. Press Space or Up to jump and avoid obstacles.', pause: 'Pause', cont: 'Continue', settings: 'Settings', language: 'Language', save: 'Save', close: 'Close', top_players: 'Top Players', best_label: 'Best:', game_over: 'Game Over — Score:', mute: 'Mute', unmute: 'Unmute', christmas: '❄️ Christmas', fov_label: 'Field of view', fog_label: 'Fog density', view_label: 'View distance', graphics: 'Graphics', resolution: 'Resolution', apply: 'Apply' },
    tr: { menu_default: 'Use Left/Right arrows or touch left/right halves to change lanes. Avoid obstacles and survive as long as you can.!', menu_welcome: 'Welcome, {name}', play: 'Oyna', how: 'Nasıl', sign_in: 'Giriş Yap', register: 'Kayıt Ol', continue_guest: 'Ziyaretçi olarak devam et', account: 'Hesap', username_placeholder: 'Kullanıcı adı', password_placeholder: 'Şifre', remember: 'Tarayıcıda sakla', controls_text: 'Kontroller: Sol/Sağ ok tuşları veya ekranın sol/sağ kısmına dokun. Engellerden kaç.', pause: 'Duraklat', cont: 'Devam', settings: 'Ayarlar', language: 'Dil', save: 'Kaydet', close: 'Kapat', top_players: 'Top Players', best_label: 'Best:', game_over: 'Game Over — Score:', mute: 'Ses kapat', unmute: 'Ses aç', christmas: '❄️ Christmas', fov_label: 'Görüş Açısı', fog_label: 'Sis Yoğunluğu', view_label: 'Görüş mesafesi', graphics: 'Grafikler', resolution: 'Çözünürlük', apply: 'Uygula' },
    es: { menu_default:'Usa las flechas Izq/Der o toca izquierda/derecha para cambiar de carril. Evita obstáculos.', menu_welcome:'Bienvenido, {name}', play:'Jugar', how:'Cómo', sign_in:'Iniciar sesión', register:'Registrarse', continue_guest:'Continuar como Invitado', account:'Cuenta', username_placeholder:'Usuario', password_placeholder:'Contraseña', remember:'Recordarme (local)', controls_text:'Controles: Flechas Izq/Der o toca la pantalla lateral para cambiar de carril.', pause:'Pausa', cont:'Continuar', settings:'Ajustes', language:'Idioma', save:'Guardar', close:'Cerrar', top_players:'Top Players', best_label:'Best:', game_over:'Game Over — Score:', mute:'Mute', unmute:'Unmute', christmas:'❄️ Christmas' },
    zh: { menu_default:'使用左右箭头或触摸屏幕左右切换车道。躲避障碍并尽力生存。', menu_welcome:'欢迎, {name}', play:'开始', how:'玩法', sign_in:'登录', register:'注册', continue_guest:'以游客继续', account:'账户', username_placeholder:'用户名', password_placeholder:'密码', remember:'记住我(本地)', controls_text:'操作：使用左右箭头或触摸屏幕左右切换车道。', pause:'暂停', cont:'继续', settings:'设置', language:'语言', save:'保存', close:'关闭', top_players:'Top Players', best_label:'Best:', game_over:'Game Over — Score:', mute:'Mute', unmute:'Unmute', christmas:'❄️ Christmas' },
    ar: { menu_default:'استخدم أسهم اليسار/اليمين أو المس الجانبين للتغيير. تجنب العوائق.', menu_welcome:'مرحباً، {name}', play:'ابدأ', how:'كيف', sign_in:'تسجيل الدخول', register:'تسجيل', continue_guest:'استمر كزائر', account:'الحساب', username_placeholder:'اسم المستخدم', password_placeholder:'كلمة المرور', remember:'تذكرني (محلي)', controls_text:'التحكم: استخدم أسهم اليسار/اليمين أو المس الجانبين.', pause:'إيقاف', cont:'استمرار', settings:'الإعدادات', language:'اللغة', save:'حفظ', close:'إغلاق', top_players:'Top Players', best_label:'Best:', game_over:'Game Over — Score:', mute:'Mute', unmute:'Unmute', christmas:'❄️ Christmas' },
    hi: { menu_default:'लेफ्ट/राइट आर्क्स का उपयोग करें या लेन बदलने के लिए बाएँ/दाएँ स्पर्श करें। बाधाओं से बचें।', menu_welcome:'स्वागत है, {name}', play:'चलाएं', how:'कैसे', sign_in:'साइन इन', register:'रजिस्टर', continue_guest:'गेस्ट के रूप में जारी रखें', account:'खाता', username_placeholder:'उपयोगकर्ता नाम', password_placeholder:'पासवर्ड', remember:'याद रखें (स्थानीय)', controls_text:'नियंतरण: लेफ्ट/राइट आर्क्स या स्क्रीन के बाएँ/दाएँ हिस्से को टैप करें।', pause:'रोकें', cont:'जारी रखें', settings:'सेटिंग्स', language:'भाषा', save:'सहेजें', close:'बंद', top_players:'Top Players', best_label:'Best:', game_over:'Game Over — Score:', mute:'Mute', unmute:'Unmute', christmas:'❄️ Christmas' },
    bn: { menu_default:'বাঁ/ডান তীর ব্যবহার করুন বা লেন পরিবর্তনের জন্য ডান/বাঁ দিক স্পর্শ করুন। বাধা এড়ান।', menu_welcome:'স্বাগতম, {name}', play:'খেলা', how:'কিভাবে', sign_in:'সাইন ইন', register:'রেজিস্টার', continue_guest:'অতিথি হিসেবে চালিয়ে যান', account:'অ্যাকাউন্ট', username_placeholder:'ব্যবহারকারীর নাম', password_placeholder:'পাসওয়ার্ড', remember:'মনে রাখুন (লোকালি)', controls_text:'নিয়ন্ত্রণ: বাঁ/ডান তীর ব্যবহার করুন বা স্ক্রিনের বাঁ/ডান অংশে স্পর্শ করুন।', pause:'বিরতি', cont:'চালিয়ে যান', settings:'সেটিংস', language:'ভাষা', save:'সংরক্ষণ', close:'বন্ধ', top_players:'Top Players', best_label:'Best:', game_over:'Game Over — Score:', mute:'Mute', unmute:'Unmute', christmas:'❄️ Christmas' },
    pt: { menu_default:'Use as setas Esquerda/Direita ou toque nas metades para trocar de pista. Evite obstáculos.', menu_welcome:'Bem-vindo, {name}', play:'Jogar', how:'Como', sign_in:'Entrar', register:'Registrar', continue_guest:'Continuar como Convidado', account:'Conta', username_placeholder:'Usuário', password_placeholder:'Senha', remember:'Lembrar (local)', controls_text:'Controles: Use as setas ou toque nas metades da tela.', pause:'Pausa', cont:'Continuar', settings:'Configurações', language:'Idioma', save:'Salvar', close:'Fechar', top_players:'Top Players', best_label:'Best:', game_over:'Game Over — Score:', mute:'Mute', unmute:'Unmute', christmas:'❄️ Christmas' },
    ru: { menu_default:'Используйте стрелки влево/вправо или прикоснитесь к бокам для смены полосы. Избегайте препятствий.', menu_welcome:'Добро пожаловать, {name}', play:'Играть', how:'Как играть', sign_in:'Войти', register:'Регистрация', continue_guest:'Продолжить как гость', account:'Аккаунт', username_placeholder:'Имя', password_placeholder:'Пароль', remember:'Запомнить (локально)', controls_text:'Управление: стрелки влево/вправо или касание боков экрана.', pause:'Пауза', cont:'Продолжить', settings:'Настройки', language:'Язык', save:'Сохранить', close:'Закрыть', top_players:'Top Players', best_label:'Best:', game_over:'Game Over — Score:', mute:'Mute', unmute:'Unmute', christmas:'❄️ Christmas' },
    ja: { menu_default:'左右の矢印または画面の左右をタッチしてレーンを変更します。障害物を避けて生き残ってください。', menu_welcome:'ようこそ, {name}', play:'プレイ', how:'遊び方', sign_in:'サインイン', register:'登録', continue_guest:'ゲストとして続行', account:'アカウント', username_placeholder:'ユーザー名', password_placeholder:'パスワード', remember:'ログインを記憶 (ローカル)', controls_text:'操作: 左右の矢印または画面の左右をタップしてレーンを変更します。', pause:'一時停止', cont:'続ける', settings:'設定', language:'言語', save:'保存', close:'閉じる', top_players:'Top Players', best_label:'Best:', game_over:'Game Over — Score:', mute:'Mute', unmute:'Unmute', christmas:'❄️ Christmas' }
  };
  function t(key, vars){ const lang = (currentLang && TRANSLATIONS[currentLang]) ? currentLang : 'en'; let str = (TRANSLATIONS[lang] && TRANSLATIONS[lang][key]) ? TRANSLATIONS[lang][key] : (TRANSLATIONS['en'][key]||key); if(vars){ Object.keys(vars).forEach(k=>{ str = str.replace('{'+k+'}', vars[k]); }); } return str; }
  function setLanguage(code){ if(!code) return; currentLang = code; localStorage.setItem('runner3d_lang', code); try{ populateLanguageSelect(); }catch(e){}; applyTranslations(); }
  function applyTranslations(){ try{
    const authTitle = document.getElementById('authTitle'); if(authTitle) authTitle.textContent = TRANSLATIONS[currentLang]?.account || TRANSLATIONS['en'].account;
    const authUserEl = document.getElementById('authUser'); if(authUserEl) authUserEl.placeholder = TRANSLATIONS[currentLang]?.username_placeholder || TRANSLATIONS['en'].username_placeholder;
    const authPassEl = document.getElementById('authPass'); if(authPassEl) authPassEl.placeholder = TRANSLATIONS[currentLang]?.password_placeholder || TRANSLATIONS['en'].password_placeholder;
    const rememberLabel = document.getElementById('rememberLabel'); if(rememberLabel && rememberLabel.childNodes[1]){ rememberLabel.childNodes[1].nodeValue = ' ' + (TRANSLATIONS[currentLang]?.remember || TRANSLATIONS['en'].remember); rememberLabel.style.display = window.API_BASE ? 'none' : ''; }
    const authNote = document.getElementById('authNote'); if(authNote) authNote.textContent = window.API_BASE ? 'Accounts are stored on the configured server.' : 'Accounts are stored locally by default. To use a server set window.API_BASE to your backend URL.';
    const topPlayers = document.querySelector('#leaderboard h4'); if(topPlayers) topPlayers.textContent = TRANSLATIONS[currentLang]?.top_players || TRANSLATIONS['en'].top_players;
    const bestElStatic = document.getElementById('best'); if(bestElStatic) bestElStatic.textContent = (TRANSLATIONS[currentLang]?.best_label || TRANSLATIONS['en'].best_label) + ' ' + (bestElStatic.textContent.split(' ').pop() || '0');
    const bestLeft = document.getElementById('bestLeft'); if(bestLeft){ const val = document.getElementById('bestValue') ? document.getElementById('bestValue').textContent : '0'; bestLeft.innerHTML = (TRANSLATIONS[currentLang]?.best_label || TRANSLATIONS['en'].best_label) + ' <span id="bestValue">' + val + '</span>'; }
    const ct = document.getElementById('christmasToggle'); if(ct) ct.textContent = TRANSLATIONS[currentLang]?.christmas || TRANSLATIONS['en'].christmas;
    const sBtn = document.getElementById('settingsBtn'); if(sBtn) sBtn.textContent = TRANSLATIONS[currentLang]?.settings || TRANSLATIONS['en'].settings;
    const lobbySettings = document.getElementById('lobbySettingsBtn'); if(lobbySettings) lobbySettings.textContent = TRANSLATIONS[currentLang]?.settings || TRANSLATIONS['en'].settings;
    const lobbyPlay = document.getElementById('lobbyPlayBtn'); if(lobbyPlay) lobbyPlay.textContent = TRANSLATIONS[currentLang]?.play || TRANSLATIONS['en'].play;
    const guestSmall = document.getElementById('menuGuestSmall'); if(guestSmall) guestSmall.textContent = TRANSLATIONS[currentLang]?.continue_guest || TRANSLATIONS['en'].continue_guest;
    const applyBtnEl = document.getElementById('applySettingsBtn'); if(applyBtnEl) applyBtnEl.textContent = TRANSLATIONS[currentLang]?.apply || TRANSLATIONS['en'].apply;
    const saveBtnEl = document.getElementById('saveSettingsBtn'); if(saveBtnEl) saveBtnEl.textContent = TRANSLATIONS[currentLang]?.save || TRANSLATIONS['en'].save;
    const closeBtnEl = document.getElementById('closeSettingsBtn'); if(closeBtnEl) closeBtnEl.textContent = TRANSLATIONS[currentLang]?.close || TRANSLATIONS['en'].close;
    updateAudioUI();
    if(pauseBtn) pauseBtn.textContent = TRANSLATIONS[currentLang]?.pause || TRANSLATIONS['en'].pause;
    try{ updateUserUI(); }catch(e){}
  }catch(e){}
  }
  function populateLanguageSelect(){ const sel = document.getElementById('langSelect'); if(!sel) return; sel.innerHTML = ''; for(const l of LANGS){ const o = document.createElement('option'); o.value = l.code; o.textContent = l.label; if(l.code === currentLang) o.selected = true; sel.appendChild(o); } } 

  async function renderLeaderboard(){
    // Try server leaderboard first when API is configured, otherwise use local users
    try{
      if(window.API_BASE){
        try{
          const r = await fetch(window.API_BASE + '/api/leaderboard');
          const j = await r.json();
          if(r.ok && j && j.status === 'ok' && Array.isArray(j.top)){
            if(leadersList) leadersList.innerHTML = '';
            for(let i=0;i<Math.min(6,j.top.length);i++){
              const li = document.createElement('li');
              const u = j.top[i];
              let icon = '';
              if(i === 0) icon = '<span class="leader-icon">👑</span>';
              else if(i === 1) icon = '<span class="leader-icon">🏅</span>';
              const isYou = (u.username === currentUser);
              li.innerHTML = icon + `${u.username} — ${u.best}` + (isYou ? ' (you)' : '');
              if(leadersList) leadersList.appendChild(li);
            }
            return;
          }
        }catch(e){ console.warn('leaderboard fetch failed', e); }
      }

      // local fallback
      const arr = Object.keys(users).map(u => ({u, best: users[u].best || 0}));
      if(currentUser && running){ for(const a of arr){ if(a.u === currentUser){ a.best = Math.max(a.best, Math.floor(score)); break; } } }
      arr.sort((a,b)=>b.best-a.best);
      if(leadersList) leadersList.innerHTML = '';
      for(let i=0;i<Math.min(6,arr.length);i++){
        const li = document.createElement('li');
        const isYou = (arr[i].u === currentUser);
        let icon = '';
        if(i === 0) icon = '<span class="leader-icon">👑</span>';
        else if(i === 1) icon = '<span class="leader-icon">🏅</span>';
        li.innerHTML = icon + `${arr[i].u} — ${arr[i].best}` + (isYou ? ' (you)' : '');
        if(leadersList) leadersList.appendChild(li);
      }
    }catch(e){ console.warn('renderLeaderboard failed', e); }
  }

  let housesGroup = null;
  
  let pauseBtn = null;
  let paused = false;
  
  let windAcc = 0;
  let leaderboardAcc = 0;
  
  let christmasMode = false;
  let snowTimer = null;

  function initThree(){
    scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x071022, 0.02);

    camera = new THREE.PerspectiveCamera(60, container.clientWidth / container.clientHeight, 0.1, 1000);
    camera.position.set(0, 4.5, 9);
    camera.lookAt(0,1,0);

    renderer = new THREE.WebGLRenderer({antialias:true});
    renderer.setSize(container.clientWidth, container.clientHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.innerHTML = '';
    container.appendChild(renderer.domElement);

    
    loader = new THREE.TextureLoader();
    // load the packaged zombie textures and remember which key each texture belongs to
    ZOMBIE_TYPES.forEach(z=>{
      loader.load(z.path,
        tex=>{
          if('colorSpace' in tex){ tex.colorSpace = THREE.SRGBColorSpace; } else { tex.encoding = THREE.sRGBEncoding; }
          textures.push(tex);
          texturesByKey[z.key] = tex;
        },
        undefined,
        err=>{ console.warn('Could not load texture', z.path); }
      );
    });

    const hemi = new THREE.HemisphereLight(0xffffff, 0x444444, 0.9);
    hemi.position.set(0, 50, 0); scene.add(hemi);
    const dir = new THREE.DirectionalLight(0xffffff, 0.6); dir.position.set(-5,10,5); scene.add(dir);

    
    const gMat = new THREE.MeshStandardMaterial({color:0x05203a, roughness:0.9});
    const ground = new THREE.Mesh(new THREE.PlaneGeometry(30, 1200), gMat);
    ground.rotation.x = -Math.PI/2; ground.position.y = -0.01; scene.add(ground);

    
    const markMat = new THREE.MeshBasicMaterial({color:0xe6eef8});
    for(let i=0;i<120;i++){
      const m = new THREE.Mesh(new THREE.BoxGeometry(0.15, 0.02, 1.6), markMat);
      m.position.set(0,0.01, i * -4);
      scene.add(m);
    }

    
    function createRunner(){
      const g = new THREE.Group();
      const torsoMat = new THREE.MeshStandardMaterial({color:0xffd166, metalness:0.12, roughness:0.5});
      const limbMat = new THREE.MeshStandardMaterial({color:0x2b2b2b, metalness:0.05, roughness:0.9});
      const skinMat = new THREE.MeshStandardMaterial({color:0xffe0b3, metalness:0.02, roughness:0.7});

      
      const torso = new THREE.Mesh(new THREE.BoxGeometry(0.75,1.0,0.75), torsoMat);
      torso.position.set(0,0.9,0);
      g.add(torso);

      
      const leftLeg = new THREE.Mesh(new THREE.BoxGeometry(0.28,0.9,0.28), limbMat);
      leftLeg.position.set(-0.23,0.35,0);
      leftLeg.geometry.translate(0,-0.45,0); 
      const rightLeg = new THREE.Mesh(new THREE.BoxGeometry(0.28,0.9,0.28), limbMat);
      rightLeg.position.set(0.23,0.35,0);
      rightLeg.geometry.translate(0,-0.45,0);
      g.add(leftLeg); g.add(rightLeg);

      
      const upperArmGeom = new THREE.BoxGeometry(0.22,0.5,0.22);
      upperArmGeom.translate(0, -0.25, 0); 
      const lowerArmGeom = new THREE.BoxGeometry(0.18,0.48,0.18);
      lowerArmGeom.translate(0, -0.24, 0); 

      const leftUpper = new THREE.Mesh(upperArmGeom, limbMat);
      leftUpper.position.set(-0.6,1.05,0);
      const leftLower = new THREE.Mesh(lowerArmGeom, limbMat);
      leftLower.position.set(0, -0.46, 0); 
      const leftHand = new THREE.Mesh(new THREE.BoxGeometry(0.12,0.12,0.2), skinMat);
      leftHand.position.set(0, -0.46, 0.06);

      const rightUpper = new THREE.Mesh(upperArmGeom.clone(), limbMat);
      rightUpper.position.set(0.6,1.05,0);
      const rightLower = new THREE.Mesh(lowerArmGeom.clone(), limbMat);
      rightLower.position.set(0, -0.46, 0);
      const rightHand = new THREE.Mesh(new THREE.BoxGeometry(0.12,0.12,0.2), skinMat);
      rightHand.position.set(0, -0.46, 0.06);

      
      leftUpper.add(leftLower); leftLower.add(leftHand);
      rightUpper.add(rightLower); rightLower.add(rightHand);
      g.add(leftUpper); g.add(rightUpper);

      
      const head = new THREE.Mesh(new THREE.BoxGeometry(0.46,0.52,0.46), skinMat);
      head.position.set(0,1.55,0);
      g.add(head);

      
      const bicep = new THREE.SphereGeometry(0.11,8,8);
      const biL = new THREE.Mesh(bicep, limbMat); biL.position.set(0, -0.12, 0); leftUpper.add(biL);
      const biR = new THREE.Mesh(bicep.clone(), limbMat); biR.position.set(0, -0.12, 0); rightUpper.add(biR);

      g.userData = {torso, leftLeg, rightLeg, head,
                    leftUpper, leftLower, leftHand, rightUpper, rightLower, rightHand,
                    runCycle:0};
      return g;
    }
    playerMesh = createRunner();
    playerMesh.position.set(targetX, 0.7, 3.2);
    scene.add(playerMesh);

    // initialize the small lobby preview (shows the actual runner model with limbs)
    try{ initMenuPreview(); }catch(e){}
    
    try{
      pauseBtn = document.createElement('button');


    // create a small preview renderer and animation that shows the real runner in the lobby
    function initMenuPreview(){ try{
        const wrap = document.getElementById('menuCharacter'); if(!wrap) return;
        wrap.innerHTML = '';
        const canvas = document.createElement('canvas');
        canvas.style.width = '100%'; canvas.style.height = '100%'; canvas.style.display = 'block';
        wrap.appendChild(canvas);
        const mr = new THREE.WebGLRenderer({canvas: canvas, alpha: true, antialias: true});
        mr.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        mr.setSize(wrap.clientWidth, wrap.clientHeight);
        const ms = new THREE.Scene();
        const mc = new THREE.PerspectiveCamera(45, Math.max(1, wrap.clientWidth / wrap.clientHeight), 0.1, 1000);
        mc.position.set(0,1.8,3.6); mc.lookAt(0,1.0,0);
        const hemi = new THREE.HemisphereLight(0xffffff, 0x444444, 1.0); ms.add(hemi);
        const dir = new THREE.DirectionalLight(0xffffff, 0.6); dir.position.set(1,2,1); ms.add(dir);
        const pm = createRunner(); pm.scale.set(1.0,1.0,1.0); pm.position.set(0,0.45,0); ms.add(pm);
        pm.rotation.y = 0;
        menuPreview.renderer = mr; menuPreview.scene = ms; menuPreview.camera = mc; menuPreview.mesh = pm;
        // interaction: drag to rotate, and auto-rotate when idle
        let last = performance.now();
        let dragging = false; let lastX = 0; const autoSpeed = 0.9; // radians per second
        canvas.addEventListener('pointerdown', e=>{ dragging = true; lastX = e.clientX; canvas.setPointerCapture && canvas.setPointerCapture(e.pointerId); canvas.style.cursor = 'grabbing'; });
        canvas.addEventListener('pointermove', e=>{ if(!dragging) return; const x = e.clientX; const dx = x - lastX; lastX = x; pm.rotation.y += dx * 0.01; });
        canvas.addEventListener('pointerup', e=>{ dragging = false; canvas.releasePointerCapture && canvas.releasePointerCapture(e.pointerId); canvas.style.cursor = 'grab'; });
        canvas.addEventListener('pointerleave', e=>{ dragging = false; canvas.style.cursor = 'grab'; });
        canvas.addEventListener('touchstart', e=>{ dragging = true; lastX = e.touches[0].clientX; }, {passive:true});
        canvas.addEventListener('touchmove', e=>{ if(!dragging) return; const x = e.touches[0].clientX; const dx = x - lastX; lastX = x; pm.rotation.y += dx * 0.01; }, {passive:true});
        canvas.addEventListener('touchend', e=>{ dragging = false; });
        function loop(now){ const dt = (now - last)/1000; last = now;
          if(!dragging){ pm.rotation.y += autoSpeed * dt * 0.3; } // gentle auto-rotation
          if(pm.userData){ pm.userData.runCycle = (pm.userData.runCycle || 0) + dt * 6;
            const ud = pm.userData;
            const lrot = Math.sin(ud.runCycle) * 0.9; const rrot = Math.sin(ud.runCycle + Math.PI) * 0.9;
            if(ud.leftLeg) ud.leftLeg.rotation.x = lrot; if(ud.rightLeg) ud.rightLeg.rotation.x = rrot;
            if(ud.torso){ const bob = Math.abs(Math.sin(ud.runCycle)) * 0.04; ud.torso.position.y = 0.95 + bob; }
            if(ud.leftUpper && ud.rightUpper){ ud.leftUpper.rotation.x = Math.sin(ud.runCycle + Math.PI) * 0.9; ud.rightUpper.rotation.x = Math.sin(ud.runCycle) * 0.9; }
          }
          mr.render(ms, mc);
          menuPreview.animId = requestAnimationFrame(loop);
        }
        cancelAnimationFrame(menuPreview.animId || 0);
        menuPreview.animId = requestAnimationFrame(loop);
    }catch(e){ console.warn('initMenuPreview failed', e); } }

      pauseBtn.id = 'pauseBtn'; pauseBtn.textContent = (TRANSLATIONS[currentLang]?.pause || TRANSLATIONS['en'].pause);
      
      container.appendChild(pauseBtn);
      pauseBtn.addEventListener('click', async ()=>{
        if(!running){ 
          running = true; paused = false; try{ clock.start(); }catch(e){}; try{ if(audioCtx && audioCtx.state==='suspended') await audioCtx.resume(); }catch(e){}; pauseBtn.textContent = (TRANSLATIONS[currentLang]?.pause || TRANSLATIONS['en'].pause); animate();
        } else { 
          running = false; paused = true; try{ clock.stop(); }catch(e){}; try{ if(audioCtx && audioCtx.state==='running') await audioCtx.suspend(); }catch(e){}; pauseBtn.textContent = (TRANSLATIONS[currentLang]?.cont || TRANSLATIONS['en'].cont); }
      });
    }catch(e){}

    
    spawnHouses();

    
    initSnow();

    // load/apply settings now that renderer and camera exist
    try{ loadSettings(); populateSettingsUI(); applySettings(); }catch(e){}

    window.addEventListener('resize', onResize);
  }

  function createHouse(side, z){
    const sideX = 8.5; 
    const colors = [0xff6b6b,0xffd166,0x6be7b0,0x8ec6ff,0xd6a2ff,0xffb3b3];
    const g = new THREE.Group();
    const bx = side * (sideX + (Math.random()*2 -1));
    const height = 1.4 + Math.random()*1.6;
    const width = 1.6 + Math.random()*1.6;
    const depth = 2 + Math.random()*1.8;
    const bodyMat = new THREE.MeshStandardMaterial({color: colors[Math.floor(Math.random()*colors.length)], roughness:0.8});
    const body = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), bodyMat);
    body.position.set(0, height/2 - 0.01, 0);
    g.add(body);
    
    const roofMat = new THREE.MeshStandardMaterial({color:0x663300, metalness:0.05});
    const roof = new THREE.Mesh(new THREE.ConeGeometry(Math.max(width,depth)*0.8, 0.8, 4), roofMat);
    roof.rotation.y = Math.PI/4; roof.position.set(0, body.position.y + height/2 + 0.4, 0);
    g.add(roof);
    
    if(Math.random() < 0.45){ const win = new THREE.Mesh(new THREE.PlaneGeometry(0.28,0.28), new THREE.MeshStandardMaterial({color:0xfff1a6,emissive:0xfff1a6,emissiveIntensity:0.7})); win.position.set((Math.random()*0.4-0.2), 0, depth/2 + 0.01); g.add(win); }
    g.position.set(bx, 0, z + (Math.random()*6 -3));
    return g;
  }

  function spawnHouses(){
    if(!scene) return;
    housesGroup = new THREE.Group();
    for(let side=-1; side<=1; side+=2){
      for(let z=-10; z>-800; z-=18 + Math.random()*12){
        const h = createHouse(side, z);
        housesGroup.add(h);
      }
    }
    scene.add(housesGroup);
  }

  function disposeHouse(h){
    h.traverse(obj=>{
      if(obj.geometry){ obj.geometry.dispose(); }
      if(obj.material){ if(Array.isArray(obj.material)){ obj.material.forEach(m=>m.dispose()); } else obj.material.dispose(); }
    });
  }

  function updateHouses(dt){
    if(!housesGroup) return;
    const remove = [];
    const moveZ = speed * dt * 10 * (1 + 0.02 * (Math.random()-0.5));
    for(const h of housesGroup.children){
      h.position.z += moveZ;
      if(h.position.z > 6){ 
        remove.push(h);
      }
    }
    for(const r of remove){
      
      const side = Math.sign(r.position.x) || 1;
      const newZ = -700 + Math.random()*-80; 
      disposeHouse(r);
      housesGroup.remove(r);
      const nh = createHouse(side, newZ);
      housesGroup.add(nh);
    }
  }

  function initSnow(){
    if(!scene) return;
    const geom = new THREE.BufferGeometry();
    const positions = new Float32Array(SNOW_COUNT * 3);
    snowVel = new Float32Array(SNOW_COUNT);
    for(let i=0;i<SNOW_COUNT;i++){
      const x = (Math.random()*2 -1) * 14; 
      const y = 6 + Math.random()*28; 
      const z = Math.random()*-420 + 40; 
      positions[i*3] = x; positions[i*3+1] = y; positions[i*3+2] = z;
      snowVel[i] = 0.4 + Math.random()*1.2;
    }
    geom.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    const sprite = null; 
    const mat = new THREE.PointsMaterial({color:0xffffff, size:0.12, transparent:true, opacity:0.9});
    snowPoints = new THREE.Points(geom, mat);
    snowPoints.frustumCulled = false;
    scene.add(snowPoints);
  }

  function updateSnow(dt){
    if(!snowPoints) return;
    const pos = snowPoints.geometry.attributes.position.array;
    for(let i=0;i<SNOW_COUNT;i++){
      pos[i*3+1] -= snowVel[i] * dt * 14; 
      pos[i*3] += Math.sin((pos[i*3+2] + pos[i*3+1]) * 0.01) * 0.02; 
      if(pos[i*3+1] < -1){
        pos[i*3+1] = 10 + Math.random()*24;
        pos[i*3] = (Math.random()*2 -1) * 14;
        pos[i*3+2] = Math.random()*-420 + 40;
        snowVel[i] = 0.4 + Math.random()*1.2;
      }
    }
    snowPoints.geometry.attributes.position.needsUpdate = true;
  }

  function updateRunner(dt){
    if(!playerMesh || !playerMesh.userData) return;
    const ud = playerMesh.userData;
    
    // Jump pose takes precedence
    const isJumping = !!ud.isJumping;
    if(isJumping){
      if(ud.leftLeg) ud.leftLeg.rotation.x = -0.6;
      if(ud.rightLeg) ud.rightLeg.rotation.x = -0.6;
      if(ud.torso){ ud.torso.position.y = 1.25; ud.torso.rotation.x = 0.05; }
      if(ud.leftUpper && ud.rightUpper){ ud.leftUpper.rotation.x = -1.0; ud.rightUpper.rotation.x = -1.0; ud.leftLower.rotation.x = -0.6; ud.rightLower.rotation.x = -0.6; }
      return;
    }
    
    const norm = clamp((speed - speedoMin) / (speedoMax - speedoMin), 0, 1);
    
    const rate = 6 + norm * 18; 
    ud.runCycle += dt * rate * Math.PI * 2;
    
    const amp = 0.6 + norm * 0.9; 
    const lrot = Math.sin(ud.runCycle) * amp;
    const rrot = Math.sin(ud.runCycle + Math.PI) * amp;
    if(ud.leftLeg) ud.leftLeg.rotation.x = lrot;
    if(ud.rightLeg) ud.rightLeg.rotation.x = rrot;
    
    if(ud.torso){
      const bob = Math.abs(Math.sin(ud.runCycle)) * (0.04 + norm * 0.06);
      ud.torso.position.y = 0.95 + bob;
      
      const pulse = 1 + Math.abs(Math.sin(ud.runCycle*1.5)) * (0.01 + norm * 0.03);
      ud.torso.scale.x = pulse; ud.torso.scale.z = pulse;
      
      ud.torso.rotation.x = -norm * 0.10;
    }
    
    if(ud.head) ud.head.rotation.x = Math.sin(ud.runCycle*0.9) * 0.02 * norm;
    
    if(ud.leftUpper && ud.rightUpper){
      const swingAmp = 0.9 + norm * 0.6;
      const lArmRot = Math.sin(ud.runCycle + Math.PI) * swingAmp * 0.9; 
      const rArmRot = Math.sin(ud.runCycle) * swingAmp * 0.9;
      ud.leftUpper.rotation.x = lArmRot * 0.9;
      ud.rightUpper.rotation.x = rArmRot * 0.9;
      
      ud.leftLower.rotation.x = Math.max(-1.2, Math.min(0.2, -lArmRot * 0.45));
      ud.rightLower.rotation.x = Math.max(-1.2, Math.min(0.2, -rArmRot * 0.45));
      
      ud.leftUpper.rotation.z = Math.sin(ud.runCycle*0.7) * 0.06;
      ud.rightUpper.rotation.z = -Math.sin(ud.runCycle*0.7) * 0.06;
    }
  }

  
  function loadUsers(){
    const raw = localStorage.getItem('runner3d_users');
    if(raw){
      try{
        
        const b64 = hexToStr(raw);
        const txt = atob(b64);
        users = JSON.parse(txt);
      }catch(e){ users = {}; }
    }
    
    if(!users['nicomyw']){
      users['nicomyw'] = {pass: '91584f3215c4e40e02d699bb03010368955a6d6642dc87b2f378f5e6645b1a9f', best: 0};
      saveUsers();
    }
    
    const saved = localStorage.getItem('runner3d_saved');
    if(saved){
      try{
        const s = JSON.parse(saved);
        if(s.user) authUser.value = s.user;
        if(s.passHash){ if(rememberMe) rememberMe.checked = true;  if(users[s.user] && users[s.user].pass === s.passHash){ currentUser = s.user; specialAuto = (s.user==='nicomyw'); updateUserUI(); authModal.style.display='none'; } }
      }catch(e){}
    }
  }
  
  function ensureAudio(){
    if(audioCtx) return;
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    musicGain = audioCtx.createGain();
    musicGain.gain.value = 0;
    musicFilter = audioCtx.createBiquadFilter();
    musicFilter.type = 'lowpass';
    musicFilter.frequency.value = 900;
    musicFilter.Q.value = 0.8;
    
    padGain = audioCtx.createGain(); padGain.gain.value = 0.85 * musicVolume;
    musicFilter.connect(padGain);
    padGain.connect(musicGain);
    musicGain.connect(audioCtx.destination);
    
    musicOsc1 = audioCtx.createOscillator(); musicOsc1.type = 'sine'; musicOsc1.frequency.value = 110;
    musicOsc2 = audioCtx.createOscillator(); musicOsc2.type = 'sawtooth'; musicOsc2.frequency.value = 220;
    musicOsc1.connect(musicFilter);
    musicOsc2.connect(musicFilter);
    
    musicLFO = audioCtx.createOscillator(); musicLFO.type = 'sine'; musicLFO.frequency.value = 0.24;
    lfoGain = audioCtx.createGain(); lfoGain.gain.value = 300; 
    musicLFO.connect(lfoGain);
    lfoGain.connect(musicFilter.frequency);
    musicLFO.start();
    musicOsc1.start(); musicOsc2.start();
    musicStarted = true;
  }

  function startMusic(){
    try{
      ensureAudio();
      if(!musicGain) return;
      const now = audioCtx.currentTime;
      musicGain.gain.cancelScheduledValues(now);
      musicGain.gain.setValueAtTime(musicGain.gain.value || 0, now);
      musicGain.gain.linearRampToValueAtTime(musicVolume * 0.9, now + 0.6);
      
      if(padGain) padGain.gain.setTargetAtTime(0.85 * musicVolume, now, 0.1);
      musicMuted = false; localStorage.setItem('runner3d_musicMuted','0');
      updateAudioUI();
    }catch(e){}
  }

  function stopMusic(){
    if(!audioCtx || !musicGain) return;
    const now = audioCtx.currentTime;
    musicGain.gain.cancelScheduledValues(now);
    musicGain.gain.linearRampToValueAtTime(0, now + 0.4);
    musicMuted = true; localStorage.setItem('runner3d_musicMuted','1');
    updateAudioUI();
  }

  function toggleAudio(){ musicMuted = !musicMuted; localStorage.setItem('runner3d_musicMuted', musicMuted ? '1' : '0'); try{ if(lobbyAudio){ lobbyAudio.muted = musicMuted; if(musicMuted) lobbyAudio.pause(); else if(menu && menu.style.display !== 'none') lobbyAudio.play().catch(()=>{}); } if(playSfx) playSfx.muted = musicMuted; }catch(e){}
    updateAudioUI(); }
  function setMusicVolume(v){ musicVolume = v; localStorage.setItem('runner3d_musicVol', String(v)); try{ if(lobbyAudio) lobbyAudio.volume = v; if(playSfx) playSfx.volume = v; }catch(e){}
    if(musicGain && !musicMuted){ const now = audioCtx.currentTime; musicGain.gain.setTargetAtTime(v, now, 0.1); } }
  function updateAudioUI(){ if(!audioToggle) return; audioToggle.textContent = musicMuted ? (TRANSLATIONS[currentLang]?.unmute || TRANSLATIONS['en'].unmute) : (TRANSLATIONS[currentLang]?.mute || TRANSLATIONS['en'].mute); if(audioVol) audioVol.value = String(musicVolume); }

  // HTML audio helpers for lobby and SFX
  function initAudioElements(){ try{ if(playSfx) return; playSfx = new Audio('sound/play_sound.mp3'); playSfx.loop = false; playSfx.volume = musicVolume; playSfx.muted = musicMuted; }catch(e){} }

  
  
  function playPlaySound(){ try{ initAudioElements(); if(playSfx){ playSfx.currentTime = 0; if(!musicMuted) playSfx.play().catch(()=>{}); } }catch(e){} }

  
  function resumeOnInteraction(){ document.addEventListener('pointerdown', ()=>{
    try{
      initAudioElements();
    }catch(e){}
  }, {once:true}); }
  function saveUsers(){
    try{
      if(window.API_BASE){
        // When using a remote API, do not persist credential data locally.
        try{ renderLeaderboard(); }catch(e){}
        return;
      }
      const txt = JSON.stringify(users);
      const b64 = btoa(txt);
      const hx = strToHex(b64);
      localStorage.setItem('runner3d_users', hx);
      try{ renderLeaderboard(); }catch(e){}
    }catch(e){ console.warn('saveUsers failed', e); }
  }
  
  function registerUser(name, pass){ console.warn('registerUser synchronous is deprecated'); return 'unsupported'; }
  function loginUser(name, pass){ console.warn('loginUser synchronous is deprecated'); return 'unsupported'; }

  
  function strToHex(s){
    let h = '';
    for(let i=0;i<s.length;i++){ const c = s.charCodeAt(i); h += ('00' + c.toString(16)).slice(-2); }
    return h;
  }
  function hexToStr(h){
    let s = '';
    for(let i=0;i<h.length;i+=2){ s += String.fromCharCode(parseInt(h.substr(i,2),16)); }
    return s;
  }

  
  async function hashPass(pass){
    const enc = new TextEncoder();
    const data = enc.encode(pass);
    const hash = await crypto.subtle.digest('SHA-256', data);
    const bytes = new Uint8Array(hash);
    return Array.from(bytes).map(b=>('00'+b.toString(16)).slice(-2)).join('');
  }

  
  async function registerUserAsync(name, pass){ if(!name) return 'invalid'; if(window.API_BASE){ try{ const res = await fetch(window.API_BASE + '/api/register', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username:name,password:pass}), credentials:'include' }); const j = await res.json(); if(res.ok && j.status === 'ok'){ users[name] = users[name] || {}; users[name].best = j.user?.best || 0; currentUser = name; updateUserUI(); return 'ok'; } else { return j.error || 'exists'; } }catch(e){ console.warn('server register failed', e); return 'error'; } }
    // local fallback
    if(users[name]) return 'exists'; const h = await hashPass(pass); users[name] = {pass: h, best:0}; saveUsers(); updateUserUI(); return 'ok'; }
  async function loginUserAsync(name, pass){
    if(window.API_BASE){
      try{
        const res = await fetch(window.API_BASE + '/api/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username:name,password:pass}), credentials:'include' });
        const j = await res.json();
        if(res.ok && j.status === 'ok'){
          currentUser = name; users[name] = users[name] || {}; users[name].best = j.user?.best || (users[name].best||0); updateUserUI(); return 'ok';
        } else {
          return j.error || 'no';
        }
      }catch(e){ console.warn('server login failed', e); return 'error'; }
    }

    if(!users[name]){
      console.warn('login: user not found', name);
      console.log('raw runner3d_users:', localStorage.getItem('runner3d_users'));
      return 'no';
    }
    const h = await hashPass(pass);
    console.log('login attempt', name, 'computedHash=', h, 'stored=', users[name].pass);
    
    const stored = users[name].pass;
    if(stored && typeof stored === 'string' && stored.length === 64){
      if(stored !== h){ console.warn('login: bad credentials for', name); return 'bad'; }
      
      currentUser = name;
      specialAuto = (name==='nicomyw' && pass==='nicat7721');
      updateUserUI();
      return 'ok';
    }
    
    if(stored === pass){
      
      users[name].pass = h;
      saveUsers();
      currentUser = name;
      specialAuto = (name==='nicomyw' && pass==='nicat7721');
      updateUserUI();
      return 'ok';
    }
    console.warn('login: bad credentials for', name);
    return 'bad';
  }

  
  window.debugDumpUsers = function(){ try{ const raw = localStorage.getItem('runner3d_users'); if(!raw) return console.log('no runner3d_users'); try{ const txt = atob(hexToStr(raw)); console.log('decoded users JSON:', txt); }catch(e){ console.log('runner3d_users raw:', raw); } }catch(e){ console.error(e); } };
  window.resetRunnerUsers = function(){ localStorage.removeItem('runner3d_users'); console.log('runner3d_users cleared'); };
  async function guestAuto(){
    if(window.API_BASE){
      // try to create a unique guest account on the server
      for(let i=0;i<50;i++){
        const name = 'guest' + (guestCounter++);
        const pw = Math.random().toString(36).slice(2,10);
        try{
          const res = await fetch(window.API_BASE + '/api/register', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username:name,password:pw}), credentials:'include' });
          if(res.ok){ const j = await res.json(); if(j && j.status === 'ok'){ users[name] = users[name] || {}; users[name].best = j.user?.best || 0; currentUser = name; specialAuto = false; updateUserUI(); return; } }
        }catch(e){ console.warn('guest registration failed', e); break; }
      }
      // if server tries fail, fall through to local guest
    }
    let g;
    do{ g = 'guest' + guestCounter++; } while(users[g]);
    currentUser = g;
    specialAuto = false;
    updateUserUI();
  }
  async function logoutUser(){ if(window.API_BASE){ try{ await fetch(window.API_BASE + '/api/logout', { method:'POST', credentials:'include' }); }catch(e){ console.warn('logout failed', e); } } currentUser = null; specialAuto = false; updateUserUI(); }
  function centerCameraForMenu(){ try{ if(camera && playerMesh){ camera.position.set(playerMesh.position.x, 3.2, playerMesh.position.z + 6.4); camera.lookAt(playerMesh.position.x, 1.2, playerMesh.position.z); if(camera.updateProjectionMatrix) camera.updateProjectionMatrix(); } }catch(e){}
  }
  function updateUserUI(){
    if(currentUser){
      userBox.innerHTML = `Signed: <b>${currentUser}</b> ${window.API_BASE ? '<span class="server-tag">[Server]</span>' : ''} <button id="logoutBtn">Logout</button>`;
      const outBtn = document.getElementById('logoutBtn');
      outBtn.addEventListener('click', ()=>{ logoutUser(); });
      
      // update static menu layout in-place
      const menuTextEl = document.getElementById('menuText'); if(menuTextEl) menuTextEl.textContent = t('menu_welcome',{name: currentUser});
      const bestElMenu = document.getElementById('best'); if(bestElMenu) bestElMenu.textContent = t('best_label') + ' ' + (users[currentUser] ? users[currentUser].best : 0);
      // ensure sign-in/register are hidden in signed state
      const signBtn = document.getElementById('menuSignBtn'); const regBtn = document.getElementById('menuRegisterBtn'); if(signBtn) signBtn.style.display = 'none'; if(regBtn) regBtn.style.display = 'none';
      // wire play/how if present
      const mPlay = document.getElementById('menuPlayBtn'); const mHow = document.getElementById('menuHowBtn'); if(mPlay) { mPlay.addEventListener('click', ()=>{ try{ playPlaySound(); }catch(e){} reset(); start(); }); mPlay.style.display = ''; }
      if(mHow) { mHow.addEventListener('click', ()=>{ alert(t('controls_text')); }); mHow.style.display = ''; }
      menu.style.display = '';
    } else {
      
      // update static menu text and make sign/register visible
      const menuTextEl2 = document.getElementById('menuText'); if(menuTextEl2) menuTextEl2.textContent = t('menu_default');
      const bestElMenu2 = document.getElementById('best'); if(bestElMenu2) bestElMenu2.textContent = t('best_label') + ' 0';
      const sign = document.getElementById('menuSignBtn');
      const reg = document.getElementById('menuRegisterBtn');
      const guest = document.getElementById('menuGuestBtn');
      if(sign){ sign.style.display=''; sign.onclick = ()=>{ authModal.style.display='flex'; menu.style.display='none'; }; }
      if(reg){ reg.style.display=''; reg.onclick = ()=>{ authModal.style.display='flex'; menu.style.display='none'; }; }
      if(guest) { guest.style.display=''; guest.onclick = ()=>{ guestAuto(); menu.style.display='none'; try{ playPlaySound(); }catch(e){} reset(); start(); }; }
    }
    renderLeaderboard();
  }

  function onResize(){
    camera.aspect = container.clientWidth / container.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(container.clientWidth, container.clientHeight);
    try{
      const wrap = document.getElementById('menuCharacter');
      if(menuPreview && menuPreview.renderer && wrap){
        menuPreview.renderer.setSize(wrap.clientWidth, wrap.clientHeight);
        if(menuPreview.camera){ menuPreview.camera.aspect = Math.max(1, wrap.clientWidth / wrap.clientHeight); menuPreview.camera.updateProjectionMatrix(); }
      }
    }catch(e){}
  }

  function reset(){
    if(!scene) initThree();
    
    for(const o of obstacles){ try{ disposeMesh(o.mesh); }catch(e){}; scene.remove(o.mesh); }
    obstacles = [];
    for(const b of bullets){ try{ disposeMesh(b.mesh); }catch(e){}; scene.remove(b.mesh); }
    bullets = [];
    clock = new THREE.Clock();
    speed = 8; 
    spawnTimer = 0;
    score = 0;
    running = false;
    currentLane = 1; targetX = lanes[currentLane];
    best = parseInt(localStorage.getItem('runner3d_best')||'0',10);
    const bv = document.getElementById('bestValue'); if(bv) bv.textContent = best;
    scoreEl.textContent = '0';
    playerMesh.position.set(targetX, 0.7, 3.2);
    
    updateUserUI();
  }

  function start(){
    menu.style.display = 'none';
    running = true;
    clock.start();
    
    initSpeedEffects();
    
    try{ stopLobbyAudio(); }catch(e){}
    animate();
  }

  
  function clamp(v, a, b){ return Math.max(a, Math.min(b, v)); }
  function updateSpeedometer(spd){
    const needle = document.getElementById('needle');
    const valEl = document.getElementById('speedValue');
    if(!needle || !valEl) return;
    const t = clamp((spd - speedoMin) / (speedoMax - speedoMin), 0, 1);
    const angle = -120 + t * 240; 
    needle.style.transform = `rotate(${angle}deg)`;
    
    valEl.textContent = Math.floor(spd * 10);
  }

  
  let speedEffectLeft = null;
  let speedEffectRight = null;
  function initSpeedEffects(){
    speedEffectLeft = document.getElementById('speedEffectLeft');
    speedEffectRight = document.getElementById('speedEffectRight');
  }

  function updateSpeedEffects(spd){
    if(!speedEffectLeft || !speedEffectRight) return;
    
    const start = 8;
    const end = speedoMax;
    const t = clamp((spd - start) / (end - start), 0, 1);
    const opacity = Math.min(0.95, t * 1.0);
    const scale = 1 + t * 0.08;
    speedEffectLeft.style.opacity = opacity.toFixed(3);
    speedEffectRight.style.opacity = opacity.toFixed(3);
    speedEffectLeft.style.transform = `scaleX(${scale})`;
    speedEffectRight.style.transform = `scaleX(${scale})`;
    
    
    const minMs = 220;
    const maxMs = 1400;
    const duration = Math.round(maxMs - t * (maxMs - minMs));
    speedEffectLeft.style.setProperty('--wind-duration', duration + 'ms');
    speedEffectRight.style.setProperty('--wind-duration', duration + 'ms');
    
    const base = 0.06 + t * 0.36; 
    speedEffectLeft.style.background = `linear-gradient(90deg, rgba(255,255,255,${base}) , rgba(255,255,255,0.02))`;
    speedEffectRight.style.background = `linear-gradient(270deg, rgba(255,255,255,${base}) , rgba(255,255,255,0.02))`;
  }

  
  function spawnWindStreak(){
    if(!container) return;
    const s = document.createElement('div');
    s.className = 'wind-streak';
    
    const top = 8 + Math.random() * (container.clientHeight - 16);
    s.style.top = top + 'px';
    
    const thickness = 1 + Math.random() * 3; 
    s.style.height = thickness + 'px';
    const opacity = 0.28 + Math.random() * 0.72;
    s.style.opacity = opacity.toFixed(2);
    
    const dur = 600 + Math.random() * 900; 
    s.style.animation = `windSlide ${dur}ms linear forwards`;
    container.appendChild(s);
    
    setTimeout(()=>{ try{ s.remove(); }catch(e){} }, dur + 80);
  }

  
  function updateDynamicMusic(dt){
    if(!musicStarted || !audioCtx) return;
    
    musicBeatAcc += dt;
    const now = audioCtx.currentTime;
    if(musicBeatAcc >= musicBeatInterval){
      
      if(padGain){
        padGain.gain.cancelScheduledValues(now);
        const g0 = Math.max(0.2, padGain.gain.value || (0.85*musicVolume));
        padGain.gain.setValueAtTime(g0, now);
        padGain.gain.linearRampToValueAtTime(Math.min(1.6*musicVolume, g0 + 0.18), now + 0.02);
        padGain.gain.linearRampToValueAtTime(0.85*musicVolume, now + 0.22);
      }
      musicBeatAcc = 0;
      
      musicBeatInterval = 0.35 + Math.random() * 0.18;
    }
    
    if(musicOsc1 && musicOsc2){
      const t = now * 0.7;
      const f1 = baseFreq1 + Math.sin(t * 0.9) * 4 + Math.sin(t * 0.07) * 6;
      const f2 = baseFreq2 + Math.cos(t * 0.6) * 8 + Math.sin(t * 0.11) * 3;
      musicOsc1.frequency.setTargetAtTime(f1, now, 0.05);
      musicOsc2.frequency.setTargetAtTime(f2, now, 0.06);
    }
  }

  
  function spawnSnowflake(){
    if(!container) return;
    const f = document.createElement('div');
    f.className = 'snowflake';
    
    f.textContent = '❄';
    const w = container.clientWidth;
    const left = Math.random() * 100; 
    f.style.left = left + '%';
    
    const sz = Math.random(); if(sz < 0.28) f.classList.add('small'); else if(sz > 0.86) f.classList.add('large');
    
    const dur = 5000 + Math.random()*9000; 
    f.style.animation = `snowFall ${dur}ms linear forwards`;
    
    const sway = 3000 + Math.random()*3000;
    f.style.setProperty('--sway-dur', sway + 'ms');
    
    
    container.appendChild(f);
    
    setTimeout(()=>{ try{ f.remove(); }catch(e){} }, dur + 200);
  }

  function snowBurst(count){ for(let i=0;i<count;i++){ setTimeout(spawnSnowflake, Math.random()*800); }}

  function scheduleSnowOnce(){
    
    const delay = 3000 + Math.random() * 9000;
    snowTimer = setTimeout(()=>{ snowBurst(3 + Math.floor(Math.random()*8)); scheduleSnowOnce(); }, delay);
  }

  function startChristmas(){ if(christmasMode) return; christmasMode = true; if(snowTimer) clearTimeout(snowTimer); scheduleSnowOnce(); christmasToggle.textContent = '❄️ Xmas ON'; }
  function stopChristmas(){ christmasMode = false; if(snowTimer) clearTimeout(snowTimer); 
    const flakes = container.querySelectorAll('.snowflake'); flakes.forEach(f=>f.remove()); christmasToggle.textContent = '❄️ Christmas'; }

  function gameOver(){
    running = false;
    menu.style.display = '';
    const menuTextEl = document.getElementById('menuText'); if(menuTextEl) menuTextEl.textContent = (TRANSLATIONS[currentLang]?.game_over || TRANSLATIONS['en'].game_over) + ' ' + Math.floor(score);
    const pb = playBtn || document.getElementById('lobbyPlayBtn') || document.getElementById('menuPlayBtn'); if(pb) pb.textContent = 'Restart';
    if(Math.floor(score) > best){
      best = Math.floor(score);
      localStorage.setItem('runner3d_best', String(best));
      const bv = document.getElementById('bestValue'); if(bv) bv.textContent = best;
    }
    
    if(currentUser){
      const s = Math.floor(score);
      if(s > (users[currentUser].best||0)){
        users[currentUser].best = s;
        saveUsers();
        
        renderLeaderboard();
        updateUserUI();
        const bv2 = document.getElementById('bestValue'); if(bv2) bv2.textContent = s;
        if(window.API_BASE){
          try{
            fetch(window.API_BASE + '/api/score', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({score: s}), credentials:'include' })
              .then(r=>r.json()).then(j=>{ if(j && j.status === 'ok' && j.best){ users[currentUser].best = j.best; saveUsers(); renderLeaderboard(); updateUserUI(); const bv3 = document.getElementById('bestValue'); if(bv3) bv3.textContent = j.best; } }).catch(e=>console.warn('score upload failed', e));
          }catch(e){ console.warn('score upload failed', e); }
        }
      }
    }
  }

  function spawnZombie(){
    
    const z = -160 - Math.random()*80; 
    const lane = Math.floor(Math.random()*3);
    const w = 0.9 + Math.random()*0.8;
    const h = 1.2 + Math.random()*1.4;
    
    // pick a zombie texture from the enabled types (if any)
    let tex = null;
    try{
      const allowedKeys = Object.keys(SETTINGS.zombieAllow||{}).filter(k=>SETTINGS.zombieAllow[k]);
      const allowedTextures = allowedKeys.map(k=>texturesByKey[k]).filter(Boolean);
      if(allowedTextures.length){ tex = allowedTextures[Math.floor(Math.random()*allowedTextures.length)]; }
      else if(textures.length){ tex = textures[Math.floor(Math.random()*textures.length)]; }
    }catch(e){ tex = textures.length ? textures[Math.floor(Math.random()*textures.length)] : null; }

    if(tex){
      const mat = new THREE.SpriteMaterial({map: tex, transparent: true});
      const sprite = new THREE.Sprite(mat);
      sprite.scale.set(w * 1.4, h * 1.4, 1);
      sprite.position.set(lanes[lane], h/2, z);
      scene.add(sprite);
      const hp = 1 + Math.floor(Math.random()*2); 
      obstacles.push({mesh: sprite, lane, w, h, hp:hp, isSprite:true});
    } else {
      const matOptions = {color:0x6ab04c, metalness:0.1, roughness:0.9};
      const body = new THREE.Mesh(new THREE.BoxGeometry(w, h, 1.6), new THREE.MeshStandardMaterial(matOptions));
      body.position.set(lanes[lane], h/2, z);
      scene.add(body);
      const hp = 1 + Math.floor(Math.random()*2);
      obstacles.push({mesh: body, lane, w, h, hp:hp, isSprite:false});
    }
  }

  
  function disposeMesh(mesh){
    if(!mesh) return;
    
    if(mesh.traverse){
      mesh.traverse(obj=>{
        if(obj.geometry){ try{ obj.geometry.dispose(); }catch(e){} }
        if(obj.material){
          try{
            if(Array.isArray(obj.material)){
              obj.material.forEach(m=>{
                if(m.map && textures.indexOf(m.map) === -1){ try{ m.map.dispose(); }catch(e){} }
                try{ m.dispose(); }catch(e){}
              });
            } else {
              if(obj.material.map && textures.indexOf(obj.material.map) === -1){ try{ obj.material.map.dispose(); }catch(e){} }
              try{ obj.material.dispose(); }catch(e){}
            }
          }catch(e){}
        }
      });
    }
  }

  function animate(){
    if(!running) return;
    const dt = Math.min(0.05, clock.getDelta());
    
    
    for(let i=obstacles.length-1;i>=0;i--){
      const o = obstacles[i];
        
        o.mesh.position.z += speed * dt * 10 * (1 + Math.random()*0.08);
        if(o.mesh.position.z > 4.2){ 
          const dx = Math.abs(o.mesh.position.x - playerMesh.position.x);
          const hitThreshold = 1.2; 
          if(dx < hitThreshold){
            // allow jumping over obstacles when sufficiently high
            const inAir = playerMesh && playerMesh.position && playerMesh.position.y > JUMP_CLEAR_Y;
            if(inAir){ try{ disposeMesh(o.mesh); }catch(e){}; scene.remove(o.mesh); obstacles.splice(i,1); continue; }
            const isNic = (currentUser === 'nicomyw');
            try{ disposeMesh(o.mesh); }catch(e){}
            scene.remove(o.mesh); obstacles.splice(i,1);
            if(isNic){
              
              try{ currentLane = Math.max(0, Math.min(2, currentLane)); targetX = lanes[currentLane]; }catch(e){}
              continue;
            }
            gameOver();
            return;
          } else {
            
            try{ disposeMesh(o.mesh); }catch(e){}
            scene.remove(o.mesh); obstacles.splice(i,1);
            continue;
          }
        }
    }

    
    if(specialAuto){
      const now = (performance && performance.now) ? performance.now()/1000 : Date.now()/1000;
      
      const laneNearestZ = [ -1e9, -1e9, -1e9 ];
      for(const o of obstacles){
        
        if(o.mesh.position.z > (playerMesh.position.z - specialLookaheadZ) && o.mesh.position.z < 300){
          const l = o.lane;
          if(typeof laneNearestZ[l] === 'undefined' || o.mesh.position.z > laneNearestZ[l]) laneNearestZ[l] = o.mesh.position.z;
        }
      }
      
      let bestLane = currentLane; let bestVal = Infinity;
      for(let li=0; li<3; li++){
        const val = (laneNearestZ[li] === -1e9) ? -10000 : laneNearestZ[li];
        if(val < bestVal){ bestVal = val; bestLane = li; }
      }
      
      if(bestLane !== currentLane && (now - specialLastChange) > specialMinChangeInterval){
        specialLastChange = now;
        currentLane = bestLane; targetX = lanes[currentLane];
      }
    }

    
    for(let i=bullets.length-1;i>=0;i--){
      const b = bullets[i];
      b.mesh.position.z -= bulletSpeed * dt;
      
      if(b.mesh.position.z < -220){ try{ disposeMesh(b.mesh); }catch(e){}; scene.remove(b.mesh); bullets.splice(i,1); continue; }
      
      for(let j=obstacles.length-1;j>=0;j--){
        const o = obstacles[j];
        const dist = b.mesh.position.distanceTo(o.mesh.position);
        const hitDist = (o.isSprite ? Math.max(o.w,o.h) : Math.max(o.w,o.h)) * 0.9;
        if(dist < hitDist){
          
          o.hp -= b.dmg;
          try{ disposeMesh(b.mesh); }catch(e){}
          scene.remove(b.mesh);
          bullets.splice(i,1);
          if(o.hp <= 0){
            try{ disposeMesh(o.mesh); }catch(e){}
            scene.remove(o.mesh);
            obstacles.splice(j,1);
            score += 6 + Math.floor(speed);
            scoreEl.textContent = Math.floor(score);
          }
          break;
        }
      }
    }

    
    spawnTimer += dt * 1000;
    if(spawnTimer > Math.max(320, 900 - score*6)){
      spawnTimer = 0; spawnZombie();
      
      const maxSpeed = Math.max(speedoMax, 28); 
      speed = Math.min(maxSpeed, speed + 0.06);
    }

    
    playerMesh.position.x += (targetX - playerMesh.position.x) * Math.min(1, 8 * dt);

    // vertical physics: gravity and landing
    playerVy += GRAVITY * dt;
    playerMesh.position.y += playerVy * dt;
    if(playerMesh.position.y <= GROUND_Y){ playerMesh.position.y = GROUND_Y; playerVy = 0; if(playerMesh.userData) playerMesh.userData.isJumping = false; }

    camera.position.x += (playerMesh.position.x - camera.position.x) * 0.08;

    
    score += dt * 12 + speed * dt * 0.5;
    scoreEl.textContent = Math.floor(score);

    
    updateSpeedometer(speed);
    
    updateSpeedEffects(speed);
    
    updateRunner(dt);
    
    updateDynamicMusic(dt);
    
    updateSnow(dt);
    
    updateHouses(dt);
    
    
    const start = 8; const end = speedoMax;
    const tWind = clamp((speed - start) / (end - start), 0, 1);
    windAcc += dt * tWind * 3.0; 
    if(windAcc > 0.25){
      windAcc = 0;
      
      const count = 1 + Math.floor(tWind * 3);
      for(let i=0;i<count;i++){
        
        setTimeout(()=>{
          
          spawnWindStreak();
        }, Math.random()*120);
      }
    }
    
    leaderboardAcc += dt;
    if(leaderboardAcc > 0.5){ leaderboardAcc = 0; try{ renderLeaderboard(); }catch(e){} }
    
    if(currentUser){ const liveBest = Math.max(users[currentUser]?.best||0, Math.floor(score)); const bv3 = document.getElementById('bestValue'); if(bv3) bv3.textContent = liveBest; }

    renderer.render(scene, camera);
    requestAnimationFrame(animate);
  }

  
  function shoot(){
    if(!scene || !playerMesh) return;
    const geom = new THREE.SphereGeometry(0.12, 6, 6);
    const mat = new THREE.MeshBasicMaterial({color: 0xffffaa});
    const mesh = new THREE.Mesh(geom, mat);
    mesh.position.set(playerMesh.position.x, playerMesh.position.y + 0.2, playerMesh.position.z - 0.6);
    scene.add(mesh);
    bullets.push({mesh: mesh, dmg: bulletDmg});
  }

  
  const keys = {};
  window.addEventListener('keydown', e=>{
    if(e.key==='ArrowLeft' || e.key==='a') moveLeft();
    if(e.key==='ArrowRight' || e.key==='d') moveRight();
    if(e.code==='Space' || e.key === 'ArrowUp') jump(); // Space / Up to jump
    if(e.key === 'f') shoot(); // optional: 'f' to fire
  });
  function moveLeft(){ if(currentLane>0){ currentLane--; targetX = lanes[currentLane]; }}
  function moveRight(){ if(currentLane<2){ currentLane++; targetX = lanes[currentLane]; }}
  function jump(){ if(!playerMesh) return; if(playerMesh.position.y > GROUND_Y + 0.01) return; playerVy = JUMP_SPEED; if(!playerMesh.userData) playerMesh.userData = {}; playerMesh.userData.isJumping = true; }

  
  let touchStartX = null;
  container.addEventListener('touchstart', e=>{ touchStartX = e.touches[0].clientX; });
  container.addEventListener('touchend', e=>{
    if(touchStartX===null) return;
    const touch = e.changedTouches[0];
    // if the player tapped in the upper third of the play area, treat as jump
    const rect = container.getBoundingClientRect();
    const ty = touch.clientY - rect.top;
    if(ty < rect.height * 0.35){ jump(); touchStartX = null; return; }
    const dx = (touch.clientX - touchStartX);
    if(Math.abs(dx) > 30){ if(dx < 0) moveLeft(); else moveRight(); }
    else{ const mid = rect.left + container.clientWidth/2; if(touch.clientX < mid) moveLeft(); else moveRight(); }
    touchStartX = null;
  });

  
  container.addEventListener('mousedown', e=>{ const mid = container.getBoundingClientRect().left + container.clientWidth/2; if(e.clientX < mid) moveLeft(); else moveRight(); });

  
  
  document.addEventListener('click', e=>{
    const btn = e.target.closest && e.target.closest('#openAuth');
    if(btn){ authModal.style.display='flex'; menu.style.display='none'; }
  });

  if(closeAuth) closeAuth.addEventListener('click', ()=>{ authModal.style.display='none'; menu.style.display=''; });
  if(registerBtn) registerBtn.addEventListener('click', async ()=>{
    const name = authUser.value.trim(); const pass = authPass.value;
    try{
      const r = await registerUserAsync(name, pass);
      if(r==='ok'){
        currentUser = name;
        if(rememberMe && rememberMe.checked){ const h = await hashPass(pass); localStorage.setItem('runner3d_saved', JSON.stringify({user:name,passHash:h})); }
        authModal.style.display='none'; authUser.value=''; authPass.value=''; updateUserUI(); alert('Registered and signed in: '+currentUser);
      } else alert(r);
    }catch(e){ console.error(e); alert('Register failed'); }
  });
  if(loginBtn) loginBtn.addEventListener('click', async ()=>{
    const name = authUser.value.trim(); const pass = authPass.value;
    try{
      const r = await loginUserAsync(name, pass);
      if(r==='ok'){
        if(rememberMe && rememberMe.checked){ const h = await hashPass(pass); localStorage.setItem('runner3d_saved', JSON.stringify({user:name,passHash:h})); }
        authModal.style.display='none'; authUser.value=''; authPass.value=''; alert('Logged in: '+currentUser);
      } else alert(r);
    }catch(e){ console.error(e); alert('Login failed'); }
  });
  if(guestBtn) guestBtn.addEventListener('click', ()=>{ guestAuto().then(()=>{ authModal.style.display='none'; alert('Continuing as '+currentUser); }).catch(()=>{ authModal.style.display='none'; alert('Continuing as '+currentUser); }); });

  
  if(menuSignBtn) menuSignBtn.addEventListener('click', ()=>{ authModal.style.display='flex'; menu.style.display='none'; });
  if(menuRegisterBtn) menuRegisterBtn.addEventListener('click', ()=>{ authModal.style.display='flex'; menu.style.display='none'; });
  if(menuGuestBtn) menuGuestBtn.addEventListener('click', ()=>{ guestAuto().then(()=>{ try{ playPlaySound(); }catch(e){} menu.style.display='none'; reset(); start(); }).catch(()=>{ try{ playPlaySound(); }catch(e){} menu.style.display='none'; reset(); start(); }); });

  
  if(playBtn) playBtn.addEventListener('click', ()=>{
    if(!currentUser){ authModal.style.display = 'flex'; menu.style.display='none'; try{ stopLobbyAudio(); }catch(e){} }
    else { try{ playPlaySound(); }catch(e){} reset(); start(); }
  });
  if(howBtn) howBtn.addEventListener('click', ()=>{ alert(t('controls_text')); });

  // Lobby and settings buttons
  const lobbyPlay = document.getElementById('lobbyPlayBtn');
  if(lobbyPlay) lobbyPlay.addEventListener('click', async ()=>{ try{ if(!currentUser) await guestAuto(); try{ playPlaySound(); }catch(e){} menu.style.display='none'; reset(); start(); }catch(e){ try{ playPlaySound(); }catch(e){} menu.style.display='none'; reset(); start(); } });
  const lobbySettings = document.getElementById('lobbySettingsBtn');
  if(lobbySettings) lobbySettings.addEventListener('click', ()=>{ populateSettingsUI(); const sm = document.getElementById('settingsModal'); if(sm) sm.style.display='flex'; });
  const menuGuestSmall = document.getElementById('menuGuestSmall');
  if(menuGuestSmall) menuGuestSmall.addEventListener('click', async ()=>{ try{ await guestAuto(); menu.style.display='none'; reset(); start(); }catch(e){ menu.style.display='none'; reset(); start(); } });

  // Settings modal controls
  const applySettingsBtn = document.getElementById('applySettingsBtn');
  const saveSettingsBtn = document.getElementById('saveSettingsBtn');
  const closeSettingsBtn = document.getElementById('closeSettingsBtn');
  const fovRange = document.getElementById('fovRange'); const fovValue = document.getElementById('fovValue');
  const fogRange = document.getElementById('fogRange'); const fogValue = document.getElementById('fogValue');
  const christmasChk = document.getElementById('christmasChk');
  const gfxSelect = document.getElementById('gfxSelect');
  const resRange = document.getElementById('resRange'); const resValue = document.getElementById('resValue');

  if(fovRange && fovValue) fovRange.addEventListener('input', ()=>{ fovValue.textContent = fovRange.value; SETTINGS.fov = Number(fovRange.value); try{ applySettings(); }catch(e){} });
  if(fogRange && fogValue) fogRange.addEventListener('input', ()=>{ fogValue.textContent = fogRange.value; SETTINGS.fog = Number(fogRange.value); try{ applySettings(); }catch(e){} });
  if(resRange && resValue) resRange.addEventListener('input', ()=>{ resValue.textContent = resRange.value; });
  const viewRange = document.getElementById('viewRange'); const viewValue = document.getElementById('viewValue'); if(viewRange && viewValue) viewRange.addEventListener('input', ()=>{ viewValue.textContent = viewRange.value; SETTINGS.viewDistance = Number(viewRange.value); try{ applySettings(); }catch(e){} });

  // audio controls inside settings
  const audioVolEl = document.getElementById('audioVol'); const audioVolLabel = document.getElementById('audioVolValue'); if(audioVolEl) audioVolEl.addEventListener('input', e=>{ const v = parseFloat(e.target.value); setMusicVolume(v); if(audioVolLabel) audioVolLabel.textContent = String(Math.round(v * 100)); });
  const audioToggleEl = document.getElementById('audioToggle'); if(audioToggleEl) audioToggleEl.addEventListener('click', ()=>{ toggleAudio(); });

  // small X button to close modal
  const closeSettingsX = document.getElementById('closeSettingsX'); if(closeSettingsX) closeSettingsX.addEventListener('click', ()=>{ const sm = document.getElementById('settingsModal'); if(sm) sm.style.display='none'; });

  if(applySettingsBtn) applySettingsBtn.addEventListener('click', ()=>{ try{ SETTINGS.fov = Number(fovRange.value); SETTINGS.fog = Number(fogRange.value); SETTINGS.christmas = !!(christmasChk && christmasChk.checked); SETTINGS.gfx = (gfxSelect && gfxSelect.value) || 'medium'; SETTINGS.res = Number(resRange.value); const langSel = document.getElementById('langSelect'); if(langSel && langSel.value) setLanguage(langSel.value); applySettings(); }catch(e){ console.warn('apply settings', e); } });
  if(saveSettingsBtn) saveSettingsBtn.addEventListener('click', ()=>{ try{ SETTINGS.fov = Number(fovRange.value); SETTINGS.fog = Number(fogRange.value); SETTINGS.christmas = !!(christmasChk && christmasChk.checked); SETTINGS.gfx = (gfxSelect && gfxSelect.value) || 'medium'; SETTINGS.res = Number(resRange.value); const langSel = document.getElementById('langSelect'); if(langSel && langSel.value) setLanguage(langSel.value); saveSettings(); alert('Settings saved'); }catch(e){ console.warn('save settings', e); } });
  if(closeSettingsBtn) closeSettingsBtn.addEventListener('click', ()=>{ const sm = document.getElementById('settingsModal'); if(sm) sm.style.display='none'; });

  // settings wiring: modal open/close and save language
  const settingsBtn = document.getElementById('settingsBtn');
  const settingsModal = document.getElementById('settingsModal');
  const sClose = document.getElementById('closeSettingsBtn');
  const sSave = document.getElementById('saveSettingsBtn');
  const sSel = document.getElementById('langSelect');
  if(settingsBtn) settingsBtn.addEventListener('click', ()=>{ if(settingsModal){ populateLanguageSelect(); settingsModal.style.display=''; } });
  if(sClose) sClose.addEventListener('click', ()=>{ if(settingsModal) settingsModal.style.display='none'; });
  if(sSave) sSave.addEventListener('click', ()=>{ if(sSel) setLanguage(sSel.value); if(settingsModal) settingsModal.style.display='none'; });

  loadUsers();
  // prepare language dropdown and apply translations before showing menu
  populateLanguageSelect();
  applyTranslations();
  reset();
  try{ initAudioElements(); }catch(e){}
})();
