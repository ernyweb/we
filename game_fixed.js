(function(){
  const container = document.getElementById('gameContainer');
  const scoreEl = document.getElementById('score');
  const bestEl = document.getElementById('bestValue');
  const bestHudEl = document.getElementById('bestValue');
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
    const zmPaths = ['zm/plantzombie.png','zm/stevezombie.png','zm/tralalelozombie.jpg','zm/sahurzombie.jpg'];
    zmPaths.forEach(p=>{
      loader.load(p,
        tex=>{
          if('colorSpace' in tex){
            tex.colorSpace = THREE.SRGBColorSpace;
          } else {
            tex.encoding = THREE.sRGBEncoding;
          }
          textures.push(tex);
        },
        undefined,
        err=>{ console.warn('Could not load texture', p); }
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
    
    try{
      pauseBtn = document.createElement('button');
      pauseBtn.id = 'pauseBtn'; pauseBtn.textContent = 'Pause';
      
      container.appendChild(pauseBtn);
      pauseBtn.addEventListener('click', async ()=>{
        if(!running){ 
          running = true; paused = false; try{ clock.start(); }catch(e){}; try{ if(audioCtx && audioCtx.state==='suspended') await audioCtx.resume(); }catch(e){}; pauseBtn.textContent = 'Pause'; animate();
        } else { 
          running = false; paused = true; try{ clock.stop(); }catch(e){}; try{ if(audioCtx && audioCtx.state==='running') await audioCtx.suspend(); }catch(e){}; pauseBtn.textContent = 'Continue'; }
      });
    }catch(e){}

    
    spawnHouses();

    
    initSnow();

    

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

  function toggleAudio(){ if(musicMuted) startMusic(); else stopMusic(); }
  function setMusicVolume(v){ musicVolume = v; localStorage.setItem('runner3d_musicVol', String(v)); if(musicGain && !musicMuted){ const now = audioCtx.currentTime; musicGain.gain.setTargetAtTime(v, now, 0.1); } }
  function updateAudioUI(){ if(!audioToggle) return; audioToggle.textContent = musicMuted ? 'Unmute' : 'Mute'; if(audioVol) audioVol.value = String(musicVolume); }

  
  function resumeOnInteraction(){ document.addEventListener('pointerdown', ()=>{
    if(!audioCtx) ensureAudio();
    if(audioCtx && audioCtx.state === 'suspended') audioCtx.resume().catch(()=>{});
    
    if(!musicMuted) startMusic();
  }, {once:true}); }
  function saveUsers(){
    try{
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

  
  async function registerUserAsync(name, pass){ if(!name) return 'invalid'; if(users[name]) return 'exists'; const h = await hashPass(pass); users[name] = {pass: h, best:0}; saveUsers(); updateUserUI(); return 'ok'; }
  async function loginUserAsync(name, pass){
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
  function guestAuto(){
    
    let g;
    do{ g = 'guest' + guestCounter++; } while(users[g]);
    currentUser = g;
    specialAuto = false;
    updateUserUI();
  }
  function logoutUser(){ currentUser = null; specialAuto = false; updateUserUI(); }
  function updateUserUI(){
    if(currentUser){
      userBox.innerHTML = `Signed: <b>${currentUser}</b> <button id="logoutBtn">Logout</button>`;
      const outBtn = document.getElementById('logoutBtn');
      outBtn.addEventListener('click', ()=>{ logoutUser(); });
      
      menu.innerHTML = `
        <h1>Runner3D</h1>
        <p id="menuText">Welcome, ${currentUser}</p>
        <div class="menuRow">
          <button id="menuPlayBtn">Play</button>
          <button id="menuHowBtn">How to</button>
        </div>
      `;
      
      const mPlay = document.getElementById('menuPlayBtn');
      const mHow = document.getElementById('menuHowBtn');
      if(mPlay) mPlay.addEventListener('click', ()=>{ reset(); start(); });
      if(mHow) mHow.addEventListener('click', ()=>{ alert('Controls: Use Left/Right arrows or touch the left/right side of the screen to change lanes. Avoid obstacles.'); });
      menu.style.display = '';
    } else {
      
      menu.innerHTML = `
        <h1>Runner3D</h1>
        <p id="menuText">Use Left/Right arrows or touch left/right halves to change lanes. Avoid obstacles and survive as long as you can.</p>
        <div class="menuRow">
          <button id="menuSignBtn">Sign in</button>
          <button id="menuRegisterBtn">Register</button>
        </div>
        <div style="margin-top:12px;color:#bcd4ff">Or <button id="menuGuestBtn" style="background:transparent;color:#ffd166;border:0;cursor:pointer">Continue as Guest</button></div>
      `;
      
      const sign = document.getElementById('menuSignBtn');
      const reg = document.getElementById('menuRegisterBtn');
      const guest = document.getElementById('menuGuestBtn');
      if(sign) sign.addEventListener('click', ()=>{ authModal.style.display='flex'; menu.style.display='none'; });
      if(reg) reg.addEventListener('click', ()=>{ authModal.style.display='flex'; menu.style.display='none'; });
      if(guest) guest.addEventListener('click', ()=>{ guestAuto(); menu.style.display='none'; reset(); start(); });
    }
    renderLeaderboard();
  }
      const container = document.getElementById('gameContainer');
      const scoreEl = document.getElementById('score');
      const bestEl = document.getElementById('bestValue');
      const bestHudEl = document.getElementById('bestValue');
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
      for(const a of arr){ if(a.u === currentUser){ a.best = Math.max(a.best, Math.floor(score)); break; } }
    }
    arr.sort((a,b)=>b.best-a.best);
    leadersList.innerHTML = '';
    for(let i=0;i<Math.min(6,arr.length);i++){
      const li = document.createElement('li');
      const isYou = (arr[i].u === currentUser);
      li.textContent = `${arr[i].u} — ${arr[i].best}` + (isYou ? ' (you)' : '');
      leadersList.appendChild(li);
    }
  }

  function onResize(){
    camera.aspect = container.clientWidth / container.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(container.clientWidth, container.clientHeight);
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
    if(bestEl) bestEl.textContent = best;
    if(bestHudEl) bestHudEl.textContent = best;
    scoreEl.textContent = '0';
    playerMesh.position.set(targetX, 0.7, 3.2);
    
    updateUserUI();
  }

  function start(){
    menu.style.display = 'none';
    running = true;
    clock.start();
    
    initSpeedEffects();
    
    try{ startMusic(); }catch(e){}
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
    document.getElementById('menuText').textContent = 'Game Over — Score: ' + Math.floor(score);
    playBtn.textContent = 'Restart';
    if(Math.floor(score) > best){
      best = Math.floor(score);
      localStorage.setItem('runner3d_best', String(best));
      if(bestEl) bestEl.textContent = best;
      if(bestHudEl) bestHudEl.textContent = best;
    }
    
    if(currentUser){
      const s = Math.floor(score);
      if(s > (users[currentUser].best||0)){
        users[currentUser].best = s;
        saveUsers();
        
        renderLeaderboard();
        updateUserUI();
        if(bestHudEl) bestHudEl.textContent = s;
      }
    }
  }

  function spawnZombie(){
    
    const z = -160 - Math.random()*80; 
    const lane = Math.floor(Math.random()*3);
    const w = 0.9 + Math.random()*0.8;
    const h = 1.2 + Math.random()*1.4;
    
    const tex = textures.length ? textures[Math.floor(Math.random()*textures.length)] : null;
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
    
    if(currentUser && bestHudEl){ const liveBest = Math.max(users[currentUser]?.best||0, Math.floor(score)); bestHudEl.textContent = liveBest; }

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
    if(e.code==='Space') shoot();
  });
  function moveLeft(){ if(currentLane>0){ currentLane--; targetX = lanes[currentLane]; }}
  function moveRight(){ if(currentLane<2){ currentLane++; targetX = lanes[currentLane]; }}

  
  let touchStartX = null;
  container.addEventListener('touchstart', e=>{ touchStartX = e.touches[0].clientX; });
  container.addEventListener('touchend', e=>{
    if(touchStartX===null) return;
    const dx = (e.changedTouches[0].clientX - touchStartX);
    if(Math.abs(dx) > 30){ if(dx < 0) moveLeft(); else moveRight(); }
    else{ const mid = container.getBoundingClientRect().left + container.clientWidth/2; if(e.changedTouches[0].clientX < mid) moveLeft(); else moveRight(); }
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
  if(guestBtn) guestBtn.addEventListener('click', ()=>{ guestAuto(); authModal.style.display='none'; alert('Continuing as '+currentUser); });

  
  if(menuSignBtn) menuSignBtn.addEventListener('click', ()=>{ authModal.style.display='flex'; menu.style.display='none'; });
  if(menuRegisterBtn) menuRegisterBtn.addEventListener('click', ()=>{ authModal.style.display='flex'; menu.style.display='none'; });
  if(menuGuestBtn) menuGuestBtn.addEventListener('click', ()=>{ guestAuto(); menu.style.display='none'; reset(); start(); });

  
  if(playBtn) playBtn.addEventListener('click', ()=>{
    if(!currentUser){ authModal.style.display = 'flex'; menu.style.display='none'; }
    else { reset(); start(); }
  });
  if(howBtn) howBtn.addEventListener('click', ()=>{ alert('Kontroller: Sol/Sağ ok tuşları veya ekranın sol/sağ kısmına dokun. Engellerden kaç.'); });

  
  loadUsers();
  reset();
})();
