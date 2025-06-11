<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>Offline VTT – Supreme v14 (Delete Tokens & Scenes)</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <style>
    :root { --ui-gray: #444; --ui-light: #f4f4f4; --accent: #e33; --handle: 12px }
    * { box-sizing: border-box; margin: 0; padding: 0 }
    html, body { height: 100%; font-family: Arial, Helvetica, sans-serif; user-select: none }
    body { display: flex; flex-direction: column }
    header { background: #222; color: #fff; padding: .6rem 1rem; font-weight: 600 }
    #toolbar { display: flex; flex-wrap: wrap; gap: .5rem; padding: .5rem; background: var(--ui-light) }
    #toolbar button, #toolbar label { display: flex; align-items: center; gap: .35rem; padding: .42rem .8rem; border: none; border-radius: 4px; background: var(--ui-gray); color: #fff; cursor: pointer; font-size: .85rem }
    #toolbar button:hover { background: #666 }
    #toolbar input[type=file] { display: none }
    #tabs { display: flex; background: #ddd; border-bottom: 1px solid #bbb; overflow-x: auto }
    #tabs .tab { position: relative; padding: .45rem .95rem; cursor: pointer; border-right: 1px solid #bbb; white-space: nowrap; display: flex; align-items: center;}
    #tabs .selected { background: #fff; font-weight: 600 }
    .close-scene { margin-left: .35rem; color: #b22; background: none; border: none; font-size: 1rem; font-weight: bold; cursor: pointer; line-height: 1; padding: 0; }
    #sceneWrap { position: relative; flex: 1 1 auto; overflow: hidden; background: #333 }
    .scene { position: absolute; inset: 0; overflow: auto }
    .scene.hidden { display: none }
    img.bg { width: 100%; height: auto; display: block; pointer-events: none }
    .token-wrap { position: absolute; width: 100px; cursor: grab }
    .token-wrap.drag { cursor: grabbing }
    .token-img { width: 100%; height: auto; display: block; pointer-events: none }
    .handle { position: absolute; right: -6px; bottom: -6px; width: var(--handle); height: var(--handle); background: #fff; border: 1px solid #000; border-radius: 50%; cursor: se-resize }
    .title { position: absolute; top: 6px; left: 8px; color: #fff; font-weight: bold; font-size: 1rem; text-shadow: 0 0 3px #000; pointer-events: none; z-index: 12 }
    .token-x {
      position: absolute;
      top: -7px; right: -7px;
      background: #fff;
      color: #a00;
      font-size: 1.15em;
      font-weight: bold;
      border: 1px solid #aaa;
      border-radius: 50%;
      width: 22px; height: 22px;
      display: flex; align-items: center; justify-content: center;
      cursor: pointer;
      z-index: 3;
      box-shadow: 0 0 3px #0006;
      transition: background .15s;
    }
    .token-x:hover { background: #fee; }
    .env-overlay {
      position: fixed;
      left: 0; top: 0; width: 100vw; height: 100vh;
      pointer-events: none;
      z-index: 11;
      opacity: 0.45;
      mix-blend-mode: multiply;
      transition: background 0.2s;
      display: none;
    }
    .env-summer { background: #ff4444; }
    .env-autumn { background: #ffbb00; }
    .env-winter { background: #0080ff; }
    .env-spring { background: #ff9800; }
    .env-night { background: #222; }
    .env-none { background: transparent; }
    #toast { position: fixed; bottom: 12px; left: 50%; transform: translateX(-50%); background: #111; color: #fff; padding: .5rem .9rem; border-radius: 4px; font-size: .85rem; opacity: 0; transition: opacity .3s }
    .modal { position: fixed; inset: 0; background: rgba(0,0,0,.55); display: flex; align-items: center; justify-content: center; z-index: 10000 }
    .box { background: #fff; padding: 1rem; border-radius: 6px; width: 260px; display: flex; flex-direction: column; gap: .7rem }
    .box select, .box input { padding: .4rem; border: 1px solid #bbb; border-radius: 4px }
    .box button { align-self: center; padding: .4rem 1rem; border: none; border-radius: 4px; background: var(--ui-gray); color: #fff; cursor: pointer }
    .box button:hover { background: #666 }
    .close-modal { position: absolute; top: 16px; right: 20px; background: none; border: none; color: #a00; font-size: 1.3rem; font-weight: bold; cursor: pointer; z-index: 11000; }
  </style>
</head>
<body>
<header>Offline Image‑Layer VTT</header>
<div id="toolbar">
  <label>Background <input id="bgInput" type="file" accept="image/*"></label>
  <label>Add Token <input id="tokenInput" type="file" accept="image/*"></label>
  <button id="addSceneBtn">＋ Scene</button>
  <button id="envBtn">🎨 Environment</button>
  <label>Scene Audio <input id="audioInput" type="file" accept="audio/*"></label>
  <button id="audioToggle">⏯️ Pause/Play</button>
  <button id="diceBtn">🎲 d10</button>
</div>
<div id="tabs"></div>
<div id="sceneWrap"></div>
<div id="envOverlay" class="env-overlay"></div>
<div id="toast"></div>
<script>
const $=q=>document.querySelector(q);
const uuid=()=>crypto.randomUUID();
const KEY='vtt-cache';
let state={scenes:[],selected:0};
function addScene(name){state.scenes.push({name,bg:'',tokens:[],env:'env-none',audio:''});state.selected=state.scenes.length-1;}
try{const raw=localStorage.getItem(KEY);if(raw)state=JSON.parse(raw);}catch{}
if(!state.scenes.length)addScene('New Scene');
const cur=()=>state.scenes[state.selected];
const save=()=>localStorage.setItem(KEY,JSON.stringify(state));
const toast=$('#toast');const notify=t=>{toast.textContent=t;toast.style.opacity=1;setTimeout(()=>toast.style.opacity=0,1400);}
const sceneWrap=$('#sceneWrap'),tabs=$('#tabs');const player=new Audio();player.loop=true;
const envOverlay=$('#envOverlay');
function updateEnvOverlay(){
  const env=cur().env||'env-none';
  envOverlay.className='env-overlay '+env;
  envOverlay.style.display=(env==='env-none')?'none':'block';
}
function renderTabs(){
  tabs.innerHTML='';
  state.scenes.forEach((s,i)=>{
    const d=document.createElement('div');
    d.className='tab'+(i===state.selected?' selected':'');
    d.innerHTML = `<span>${s.name}</span>` +
      (state.scenes.length>1?`<button class="close-scene" title="Delete Scene" onclick="window._deleteScene(${i},event)">✖</button>`:'');
    d.onclick=ev=>{
      if(ev.target.classList.contains('close-scene'))return;
      state.selected=i;renderScenes();renderTabs();
    };
    d.ondblclick=()=>{const n=prompt('Scene title',s.name);if(n){s.name=n;renderTabs();renderScenes();save();}};
    tabs.appendChild(d);
  });
}
window._deleteScene = function(idx,ev){
  ev.stopPropagation();
  if(state.scenes.length<=1) return;
  state.scenes.splice(idx,1);
  if(state.selected>=state.scenes.length) state.selected=state.scenes.length-1;
  renderTabs(); renderScenes(); save();
};
function mkToken(t){
  const w=document.createElement('div');
  w.className='token-wrap';
  w.dataset.id=t.id;
  w.style.cssText=`left:${t.x}px;top:${t.y}px;width:${t.w}px;z-index:${t.z||0}`;
  const img=document.createElement('img');
  img.src=t.src;
  img.className='token-img';
  w.appendChild(img);
  // X button
  const xbtn = document.createElement('div');
  xbtn.className = 'token-x';
  xbtn.title = 'Delete Token';
  xbtn.textContent = '✖';
  xbtn.onclick = ev => {
    ev.stopPropagation();
    let arr = cur().tokens;
    arr.splice(arr.findIndex(k=>k.id===t.id),1);
    renderScenes();
    save();
  };
  w.appendChild(xbtn);
  const h=document.createElement('div');
  h.className='handle';
  w.appendChild(h);
  drag(w);resize(w,h);
  return w;
}
function renderScenes(){
  sceneWrap.innerHTML='';
  state.scenes.forEach((s,i)=>{
    const sc=document.createElement('div');
    sc.className='scene'+(i===state.selected?'':' hidden');
    if(s.bg){
      const bg=document.createElement('img');
      bg.src=s.bg;
      bg.className='bg';
      sc.appendChild(bg);
    }
    s.tokens.forEach(t=>sc.appendChild(mkToken(t)));
    const title=document.createElement('div');
    title.className='title';
    title.textContent=s.name;
    sc.appendChild(title);
    sceneWrap.appendChild(sc);
  });
  setAudio();save();updateEnvOverlay();
}
function setAudio(){
  player.pause();
  if(cur().audio){
    player.src=cur().audio;
    player.play();
    $('#audioToggle').textContent='⏸️ Pause';
  }else{
    $('#audioToggle').textContent='▶️ Play';
  }
}
function drag(w){let ox,oy;w.onpointerdown=e=>{
  if(e.target.classList.contains('handle')||e.target.classList.contains('token-x'))return;
  ox=e.offsetX;oy=e.offsetY;
  w.setPointerCapture(e.pointerId);
  w.classList.add('drag');
  w.onpointermove=ev=>{
    const x=ev.clientX+sceneWrap.scrollLeft-ox,y=ev.clientY+sceneWrap.scrollTop-oy;
    w.style.left=x+'px';w.style.top=y+'px';
    const t=cur().tokens.find(k=>k.id===w.dataset.id);t.x=x;t.y=y;
  };
  w.onpointerup=()=>{w.onpointermove=null;w.onpointerup=null;w.classList.remove('drag');save();};
};}
function resize(w,h){let sx,iw;h.onpointerdown=e=>{
  e.stopPropagation();sx=e.clientX;iw=w.offsetWidth;
  h.setPointerCapture(e.pointerId);
  h.onpointermove=ev=>{
    const nw=Math.max(20,iw+(ev.clientX-sx));
    w.style.width=nw+'px';
    cur().tokens.find(k=>k.id===w.dataset.id).w=nw;
  };
  h.onpointerup=()=>{h.onpointermove=null;h.onpointerup=null;save();};
};}
$('#bgInput').onchange=e=>{
  const f=e.target.files[0];
  if(f){cur().bg=URL.createObjectURL(f);renderScenes();}
};
$('#tokenInput').onchange=e=>{
  const f=e.target.files[0];
  if(f){cur().tokens.push({id:uuid(),src:URL.createObjectURL(f),x:0,y:0,w:100,z:0});renderScenes();}
};
$('#addSceneBtn').onclick=()=>{
  const n=prompt('Scene title','New Scene');
  addScene(n||'Scene '+state.scenes.length);renderTabs();renderScenes();notify('Scene added');
};
$('#envBtn').onclick=()=>{
  const modal=document.createElement('div');
  modal.className='modal';
  modal.innerHTML=`<div class="box"><h3>Environment Filter</h3>
    <select id='envSel'>
      <option value='env-none'>None</option>
      <option value='env-summer'>Verão (Red)</option>
      <option value='env-autumn'>Outono (Yellow)</option>
      <option value='env-winter'>Inverno (Blue)</option>
      <option value='env-spring'>Primavera (Orange)</option>
      <option value='env-night'>Night (Dark)</option>
    </select>
    <button id='ok'>Apply</button></div>`;
  document.body.appendChild(modal);
  modal.querySelector('#envSel').value=cur().env||'env-none';
  modal.querySelector('#ok').onclick=()=>{
    cur().env=modal.querySelector('#envSel').value;
    renderScenes();modal.remove();
  };
  modal.onclick=e=>{if(e.target===modal)modal.remove();};
};
$('#audioInput').onchange=e=>{
  const f=e.target.files[0];
  if(f){cur().audio=URL.createObjectURL(f);setAudio();save();notify('Audio loaded');}
};
$('#audioToggle').onclick=()=>{
  if(player.paused){
    player.play();$('#audioToggle').textContent='⏸️ Pause';
  }else{
    player.pause();$('#audioToggle').textContent='▶️ Play';
  }
};
$('#diceBtn').onclick=()=>{
  const modal=document.createElement('div');
  modal.className='modal';
  modal.innerHTML=`<div class="box" style="position:relative;">
    <button class="close-modal" title="Close" style="position:absolute;right:8px;top:3px;">✖</button>
    <h3>Roll d10</h3>
    <input id='cnt' type='number' min='1' max='20' value='1'>
    <button id='go'>Roll</button>
    <div id='diceResult' style="margin-top:8px;font-size:1rem;"></div>
    </div>`;
  document.body.appendChild(modal);
  modal.querySelector('.close-modal').onclick=()=>{modal.remove();}
  modal.querySelector('#go').onclick=()=>{
    const n=Math.min(20,Math.max(1,parseInt(modal.querySelector('#cnt').value||'1',10)));
    const rolls=Array.from({length:n},()=>1+Math.random()*10|0);
    modal.querySelector('#diceResult').textContent=`${n}d10 → [${rolls.join(', ')}] = ${rolls.reduce((a,b)=>a+b)}`;
  };
};
renderTabs();renderScenes();
window.addEventListener('resize',updateEnvOverlay);
</script>
</body>
</html>
