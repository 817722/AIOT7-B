(() => {
'use strict';const main=document.querySelector('#main-content');if(!main)return;const bridge=main.dataset.bridgeNo,$=id=>document.getElementById(id);let current=null;let selectionToken=0;let comparisonToken=0;let comparisonRecords=[];const comparisonCache=new Map();const csrf=document.querySelector('meta[name="_csrf"]')?.content, csrfHeader=document.querySelector('meta[name="_csrf_header"]')?.content;
async function json(url,opts={}){const h={Accept:'application/json',...(opts.headers||{})};if(opts.method&&csrf&&csrfHeader)h[csrfHeader]=csrf;const r=await fetch(url,{credentials:'same-origin',...opts,headers:h});if(!r.ok)throw Error(`요청 처리 실패 (${r.status})`);return r.status===204?null:r.json();}
function text(id,value){const e=$(id);if(e)e.textContent=value||'-';}
function mediaKind(m){
 const type=String(m?.type||'').toUpperCase();
 if(type==='IMAGE'||type==='VIDEO')return type;
 const mime=String(m?.mimeType||'').toLowerCase();
 if(mime.startsWith('image/'))return 'IMAGE';
 if(mime.startsWith('video/'))return 'VIDEO';
 const filename=String(m?.originName||'').toLowerCase();
 if(/\.(jpe?g|png|webp|gif|bmp)$/i.test(filename))return 'IMAGE';
 if(/\.(mp4|webm|mov|avi|mkv)$/i.test(filename))return 'VIDEO';
 return 'OTHER';
}
function uniqueMedia(list){
 const seen=new Set();
 return (Array.isArray(list)?list:[]).filter(m=>{
  if(!m||(!m.no&&!m.id))return false;
  const key=m.no!=null?`no:${m.no}`:`id:${m.id}`;
  if(seen.has(key))return false;
  seen.add(key);return true;
 });
}
function mediaUrl(no){return `/api/media/file/${encodeURIComponent(no)}`;}

// 다른 점검 기록의 파일은 비교 목적으로만 표시합니다. 이 화면에서는 삭제하지 않습니다.
function readableDate(value){return value||'날짜 미등록';}
function fileSignature(m){return `${mediaKind(m)}|${String(m.originName||'').trim().toLowerCase()}|${m.fileSize??''}`;}
function existingSignatures(items){return new Set(items.map(fileSignature));}
function compareCard(m, fingerprints){
 const type=mediaKind(m),card=document.createElement('div');card.className='in-compare-card';
 if(type==='IMAGE'){
  const a=document.createElement('a');a.href=mediaUrl(m.no);a.target='_blank';a.rel='noopener';
  const img=document.createElement('img');img.loading='lazy';img.src=a.href;img.alt=m.originName||'현장 사진';a.append(img);card.append(a);
 }else{
  const video=document.createElement('video');video.controls=true;video.preload='none';video.playsInline=true;video.src=mediaUrl(m.no);card.append(video);
 }
 const name=document.createElement('strong');name.textContent=m.originName||`${type==='IMAGE'?'사진':'영상'} #${m.no}`;card.append(name);
 const meta=document.createElement('small');meta.textContent=[m.createdAt?String(m.createdAt).slice(0,10):'',m.fileSize!=null?`${(Number(m.fileSize)/1024/1024).toFixed(1)} MB`:'' ].filter(Boolean).join(' · ');card.append(meta);
 if(fingerprints.has(fileSignature(m))){const badge=document.createElement('span');badge.className='in-duplicate-hint';badge.textContent='선택한 기록과 파일명·크기가 같음';card.append(badge);}
 return card;
}
function resetComparison(records,selectedNo){
 comparisonRecords=(Array.isArray(records)?records:[]).filter(r=>String(r.no)!==String(selectedNo));
 comparisonToken++;
 const selector=$('in-compare-record'),panel=$('in-comparison-panel'),result=$('in-compare-results');
 selector.replaceChildren();const option=document.createElement('option');option.value='';option.textContent='비교할 다른 점검 기록 선택';selector.append(option);
 for(const r of comparisonRecords){const o=document.createElement('option');o.value=String(r.no);o.textContent=`${readableDate(r.inspectionDate)} · ${r.title||'제목 없음'}`;selector.append(o);}
 result.replaceChildren();panel.hidden=false;
 if(!comparisonRecords.length)result.textContent='이 교량에는 비교할 다른 점검 기록이 없습니다.';
}
async function renderComparison(){
 const panel=$('in-comparison-panel');if(panel.hidden)return;
 const no=$('in-compare-record').value,result=$('in-compare-results');const token=++comparisonToken;
 result.replaceChildren();if(!no){result.textContent='기록을 선택하면 해당 기록에 등록된 사진과 영상을 확인할 수 있습니다.';return;}
 result.textContent='첨부 자료를 불러오는 중...';
 try{
  let media=comparisonCache.get(no);
  if(!media){media=uniqueMedia(await json(`/api/media/INSPECTION/${encodeURIComponent(no)}`));comparisonCache.set(no,media);}
  if(token!==comparisonToken)return;
  const currentMedia=comparisonCache.get(String(current?.no))||[];
  const fingerprints=existingSignatures(currentMedia);
  result.replaceChildren();
  const record=comparisonRecords.find(r=>String(r.no)===no);
  const title=document.createElement('p');title.className='in-compare-label';title.textContent=`${record?.title||'점검 기록'}의 첨부 자료 · ${media.length}개`;result.append(title);
  if(!media.length){const empty=document.createElement('p');empty.textContent='이 기록에 등록된 자료가 없습니다.';result.append(empty);return;}
  for(const [type,label] of [['IMAGE','현장 증거 사진'],['VIDEO','드론 영상']]){
   const files=media.filter(m=>mediaKind(m)===type);if(!files.length)continue;
   const h=document.createElement('h4');h.textContent=`${label} (${files.length})`;result.append(h);
   const grid=document.createElement('div');grid.className='in-comparison-grid';files.forEach(m=>grid.append(compareCard(m,fingerprints)));result.append(grid);
  }
 }catch(e){if(token===comparisonToken)result.textContent=`자료 조회 실패: ${e.message}`;}
}

function addMedia(m,target){
 const card=document.createElement('div');card.className='in-photo-entry';
 const a=document.createElement('a');a.className='in-photo-card';a.href=mediaUrl(m.no);a.target='_blank';a.rel='noopener';
 const img=document.createElement('img');img.src=a.href;img.alt=m.originName||'점검 사진';img.loading='lazy';
 const name=document.createElement('span');name.textContent=m.originName||`사진 #${m.no}`;
 a.append(img,name);card.append(a);
 // DELETE /api/media/{mediaNo} is restricted to corporate admins in the existing backend.
 if($('in-detail-delete')){
  const remove=document.createElement('button');remove.type='button';remove.className='in-media-delete in-photo-delete';remove.textContent='사진 삭제';
  remove.setAttribute('aria-label',`${m.originName||'현장 증거 사진'} 삭제`);
  remove.addEventListener('click',async()=>{
   if(!confirm(`사진 '${m.originName||m.no}'을(를) 삭제하시겠습니까? 삭제 후 복구할 수 없습니다.`))return;
   remove.disabled=true;
   try{
    await json(`/api/media/${encodeURIComponent(m.no)}`,{method:'DELETE'});
    card.remove();comparisonCache.set(String(current?.no),(comparisonCache.get(String(current?.no))||[]).filter(x=>x.no!==m.no));
    if(!target.querySelector('.in-photo-entry'))target.textContent='등록된 현장 사진이 없습니다.';
    text('in-detail-message','사진이 삭제되었습니다.');
   }catch(e){text('in-detail-message',`사진 삭제 실패: ${e.message}`);remove.disabled=false;}
  });
  card.append(remove);
 }
 target.append(card);
}
async function select(no){const selectedToken=++selectionToken;comparisonToken++;const panel=$('in-record-detail');panel.hidden=false;text('in-detail-message','기록 조회 중...');document.querySelectorAll('.in-record-row').forEach(x=>x.classList.toggle('is-selected',x.dataset.recordNo===String(no)));try{const all=await json(`/api/inspections/bridge/${encodeURIComponent(bridge)}`);const found=all.find(r=>String(r.no)===String(no));if(!found)throw Error('선택한 점검 기록을 찾을 수 없습니다.');const record=await json(`/api/inspections/${encodeURIComponent(found.id)}`);if(selectedToken!==selectionToken)return;current=record;text('detail-title',record.title);text('detail-type',record.inspectionType);text('detail-date',record.inspectionDate);text('detail-status',record.status);text('detail-inspector',String(record.inspectorNo||''));text('detail-overall-result',record.overallResult);const opinion=$('detail-review-opinion');if(opinion){const available=Object.prototype.hasOwnProperty.call(record,'reviewOpinion');opinion.textContent=available?(record.reviewOpinion?.trim()||'-'):'-';opinion.classList.toggle('in-data-unavailable',!available);}text('detail-memo',record.memo);$('in-detail-edit').href=`/inspections/new?id=${encodeURIComponent(record.id)}`;history.replaceState(null,'',`/inspections/${bridge}?inspectionId=${encodeURIComponent(record.id)}`);
const media=uniqueMedia(await json(`/api/media/INSPECTION/${encodeURIComponent(no)}`));if(selectedToken!==selectionToken)return;comparisonCache.set(String(no),media);resetComparison(all,no);const gallery=$('in-detail-photos'),videoList=$('in-detail-videos'),player=$('in-detail-player');gallery.replaceChildren();videoList.replaceChildren();player.pause();player.removeAttribute('src');player.load();player.hidden=true;$('in-player-empty').hidden=false;const images=media.filter(m=>mediaKind(m)==='IMAGE');if(!images.length)gallery.textContent='등록된 현장 사진이 없습니다.';else images.forEach(m=>addMedia(m,gallery));const actualVideos=media.filter(m=>mediaKind(m)==='VIDEO');if(!actualVideos.length){const li=document.createElement('li');li.textContent='등록된 드론 영상이 없습니다.';videoList.append(li);}for(const v of actualVideos){const li=document.createElement('li'),entry=document.createElement('div'),button=document.createElement('button');entry.className='in-video-entry';button.type='button';button.className='in-video-choice';button.textContent=v.originName||`영상 #${v.no}`;button.addEventListener('click',()=>{player.src=mediaUrl(v.no);player.hidden=false;$('in-player-empty').hidden=true;player.load();});entry.append(button);const remove=document.createElement('button');remove.type='button';remove.className='in-media-delete';remove.textContent='삭제';remove.setAttribute('aria-label',`${v.originName||'드론 영상'} 삭제`);remove.addEventListener('click',async()=>{if(!confirm(`영상 '${v.originName||v.no}'을(를) 삭제하시겠습니까? 삭제 후 복구할 수 없습니다.`))return;remove.disabled=true;try{await json(`/api/media/${encodeURIComponent(v.no)}`,{method:'DELETE'});if(player.src.includes(`/api/media/file/${v.no}`)){player.pause();player.removeAttribute('src');player.load();player.hidden=true;$('in-player-empty').hidden=false;}li.remove();comparisonCache.set(String(current?.no),(comparisonCache.get(String(current?.no))||[]).filter(x=>x.no!==v.no));if(!videoList.children.length){const empty=document.createElement('li');empty.textContent='등록된 드론 영상이 없습니다.';videoList.append(empty);}text('in-detail-message','영상이 삭제되었습니다.');}catch(e){text('in-detail-message',`영상 삭제 실패: ${e.message}`);remove.disabled=false;}});if($('in-detail-delete'))entry.append(remove);li.append(entry);videoList.append(li);}text('in-detail-message','점검 기록 및 첨부 자료');panel.scrollIntoView({behavior:'smooth',block:'start'});}catch(e){text('in-detail-message',e.message);}}
$('in-compare-record')?.addEventListener('change',renderComparison);
document.querySelectorAll('.in-record-row').forEach(b=>b.addEventListener('click',()=>select(b.dataset.recordNo)));
$('in-detail-delete')?.addEventListener('click',async()=>{if(!current||!confirm(`'${current.title}' 점검 기록을 삭제하시겠습니까? 첨부파일이 있으면 먼저 삭제해야 합니다.`))return;try{await json(`/api/inspections/${encodeURIComponent(current.id)}`,{method:'DELETE'});location.assign('/inspections');}catch(e){const m=$('in-delete-message');m.textContent=`삭제 실패: ${e.message}. 첨부파일이 있다면 관리자 권한으로 먼저 정리해야 합니다.`;m.hidden=false;}});
(async()=>{const id=new URLSearchParams(location.search).get('inspectionId');if(!id)return;try{const list=await json(`/api/inspections/bridge/${encodeURIComponent(bridge)}`);const r=list.find(x=>x.id===id);if(r)await select(r.no);}catch(e){text('in-detail-message',e.message);}})();
})();
