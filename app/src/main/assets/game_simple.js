// Offline zombie runner: 3 lanes, avoid zombie sprites, houses on the sides.
(function(){
  const container = document.getElementById('gameContainer');
  const scoreEl = document.getElementById('score');
  const bestEl = document.getElementById('best');
  const menu = document.getElementById('menu');
  const playBtn = document.getElementById('playBtn');

  let scene, camera, renderer;
  let playerMesh;
  let zombies = [];
  let buildings = [];
  let clock, speed, spawnTimer, score, running;
  let best = parseInt(localStorage.getItem('runner3d_best')) || 0;

  const lanes = [-2.2, 0, 2.2];
  let currentLane = 1;
  let targetX = lanes[currentLane];

  // Jump physics (light)
  let playerVy = 0;
  const GRAVITY = -28;
  const JUMP_SPEED = 8.0;
  const GROUND_Y = 0.8;

  // Zombie textures
  const zombiePaths = [
    'zm/plantzombie.png',
    'zm/stevezombie.png',
    'zm/tralalelozombie.jpg',
    'zm/sahurzombie.jpg',
    'zm/bananinizombie.jfif'
  ];
  let zombieTextures = [];

  function loadZombieTextures(){
    const loader = new THREE.TextureLoader();
    zombieTextures = zombiePaths.map(p => loader.load(p));
  }

  function init(){
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x041019);
    scene.fog = new THREE.FogExp2(0x041019, 0.035);

    camera = new THREE.PerspectiveCamera(65, container.clientWidth / container.clientHeight, 0.1, 1000);
    camera.position.set(0, 5, 10);
    camera.lookAt(0, 2, -5);

    renderer = new THREE.WebGLRenderer({antialias: false});
    renderer.setSize(container.clientWidth, container.clientHeight);
    renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1));
    container.appendChild(renderer.domElement);

    // Lights
    scene.add(new THREE.AmbientLight(0x406080, 0.8));
    const dirLight = new THREE.DirectionalLight(0xffffff, 0.9);
    dirLight.position.set(6, 12, 4);
    scene.add(dirLight);

    // Road
    addRoad();
    // Buildings
    addBuildings();

    // Player
    const playerGeo = new THREE.BoxGeometry(0.8, 1.4, 0.8);
    const playerMat = new THREE.MeshStandardMaterial({color: 0x00c853, metalness: 0.1, roughness: 0.6});
    playerMesh = new THREE.Mesh(playerGeo, playerMat);
    playerMesh.position.set(0, GROUND_Y, 4);
    scene.add(playerMesh);

    clock = new THREE.Clock();
    speed = 12;
    spawnTimer = 0;
    score = 0;
    running = false;

    loadZombieTextures();

    window.addEventListener('resize', onResize);
    document.addEventListener('keydown', onKeyDown);
    container.addEventListener('touchstart', onTouch);

    bestEl.textContent = 'Best: ' + best;
    menu.style.display = 'flex';
  }

  function addRoad(){
    const roadGeo = new THREE.PlaneGeometry(8, 4000);
    const roadMat = new THREE.MeshStandardMaterial({color: 0x0a0a0f, side: THREE.DoubleSide});
    const road = new THREE.Mesh(roadGeo, roadMat);
    road.rotation.x = -Math.PI / 2;
    road.position.z = -2000;
    road.receiveShadow = true;
    scene.add(road);

    // Lane lines
    const lineGeo = new THREE.PlaneGeometry(0.08, 4000);
    const lineMat = new THREE.MeshBasicMaterial({color: 0xffffff, side: THREE.DoubleSide});
    const line1 = new THREE.Mesh(lineGeo, lineMat);
    line1.rotation.x = -Math.PI / 2;
    line1.position.set(-1.1, 0.01, -2000);
    scene.add(line1);
    const line2 = line1.clone();
    line2.position.x = 1.1;
    scene.add(line2);
  }

  function addBuildings(){
    const colors = [0x1c1c2b, 0x182033, 0x11202f];
    for(let i = 0; i < 60; i++){
      const height = 4 + Math.random() * 8;
      const depth = 3 + Math.random() * 2;
      const geo = new THREE.BoxGeometry(2.5, height, depth);
      const mat = new THREE.MeshStandardMaterial({color: colors[i % colors.length], metalness: 0.1, roughness: 0.8});
      const left = new THREE.Mesh(geo, mat);
      left.position.set(-6, height / 2, -i * 12);
      scene.add(left);
      buildings.push(left);

      const right = left.clone();
      right.position.x = 6;
      scene.add(right);
      buildings.push(right);
    }
  }

  function recycleBuildings(delta){
    const limit = 10;
    buildings.forEach(b => {
      b.position.z += speed * delta;
      if(b.position.z > limit){
        b.position.z -= 12 * 60; // wrap back
      }
    });
  }

  function onResize(){
    camera.aspect = container.clientWidth / container.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(container.clientWidth, container.clientHeight);
  }

  function onKeyDown(e){
    if(!running) return;
    if(e.key === 'ArrowLeft'){
      currentLane = Math.max(0, currentLane - 1);
      targetX = lanes[currentLane];
    } else if(e.key === 'ArrowRight'){
      currentLane = Math.min(2, currentLane + 1);
      targetX = lanes[currentLane];
    } else if(e.key === ' ' || e.key === 'ArrowUp'){
      if(playerMesh.position.y <= GROUND_Y + 0.05){
        playerVy = JUMP_SPEED;
      }
    }
  }

  function onTouch(e){
    if(!running) return;
    const rect = container.getBoundingClientRect();
    const x = e.touches[0].clientX - rect.left;
    if(x < rect.width / 2){
      currentLane = Math.max(0, currentLane - 1);
    } else {
      currentLane = Math.min(2, currentLane + 1);
    }
    targetX = lanes[currentLane];
  }

  function startGame(){
    menu.style.display = 'none';
    running = true;
    score = 0;
    speed = 12;
    spawnTimer = 0;
    playerMesh.position.set(0, GROUND_Y, 4);
    playerVy = 0;
    currentLane = 1;
    targetX = lanes[currentLane];
    zombies.forEach(z => scene.remove(z.mesh));
    zombies = [];
    clock.start();
    animate();
  }

  function spawnZombie(){
    if(!zombieTextures.length) return;
    const tex = zombieTextures[Math.floor(Math.random() * zombieTextures.length)];
    const mat = new THREE.MeshBasicMaterial({map: tex, transparent: true, side: THREE.DoubleSide});
    const geo = new THREE.PlaneGeometry(1.6, 2.2);
    const mesh = new THREE.Mesh(geo, mat);
    mesh.position.set(lanes[Math.floor(Math.random() * 3)], 1.1, -45 - Math.random() * 10);
    scene.add(mesh);
    zombies.push({mesh});
  }

  function moveZombies(delta){
    zombies.forEach(z => {
      z.mesh.position.z += speed * delta * 1.25;
    });
    zombies = zombies.filter(z => {
      if(z.mesh.position.z > 12){
        scene.remove(z.mesh);
        return false;
      }
      return true;
    });
  }

  function checkCollision(){
    const px = playerMesh.position.x;
    const py = playerMesh.position.y;
    const pz = playerMesh.position.z;
    for(const z of zombies){
      const m = z.mesh.position;
      const dx = Math.abs(px - m.x);
      const dy = Math.abs(py - m.y);
      const dz = Math.abs(pz - m.z);
      if(dx < 0.9 && dy < 1.4 && dz < 0.8){
        return true;
      }
    }
    return false;
  }

  function gameOver(){
    running = false;
    if(score > best){
      best = score;
      localStorage.setItem('runner3d_best', best);
      bestEl.textContent = 'Best: ' + best;
    }
    alert('Game Over! Score: ' + score);
    menu.style.display = 'flex';
  }

  function animate(){
    if(!running) return;
    requestAnimationFrame(animate);

    const delta = clock.getDelta();

    // Horizontal move
    const dx = targetX - playerMesh.position.x;
    playerMesh.position.x += dx * 7 * delta;

    // Jump
    playerVy += GRAVITY * delta;
    playerMesh.position.y += playerVy * delta;
    if(playerMesh.position.y < GROUND_Y){
      playerMesh.position.y = GROUND_Y;
      playerVy = 0;
    }

    // Buildings scrolling
    recycleBuildings(delta);

    // Zombies
    spawnTimer += delta;
    if(spawnTimer > 1.25){
      spawnZombie();
      spawnTimer = 0;
    }
    moveZombies(delta);

    // Score & speed
    score += Math.floor(speed * delta * 2.2);
    speed = Math.min(28, 12 + score * 0.0015);
    scoreEl.textContent = 'Score: ' + score;

    if(checkCollision()){
      gameOver();
    }

    renderer.render(scene, camera);
  }

  playBtn.addEventListener('click', startGame);
  init();
})();
