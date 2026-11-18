(() => {
'use strict';const main=document.querySelector('#main-content');if(!main)return;const bridge=main.dataset.bridgeNo,$=id=>document.getElementById(id);let current=null;let selectionToken=0;let comparisonToken=0;let comparisonRecords=[];const comparisonCache=new Map();const csrf=document.querySelector('meta[name="_csrf"]')?.content, csrfHeader=document.querySelector('meta[name="_csrf_header"]')?.content;
async function json(url,opts={}){const h={Accept:'application/json',...(opts.headers||{})};if(opts.method&&csrf&&csrfHeader)h[csrfHeader]=csrf;const r=await fetch(url,{credentials:'same-origin',...opts,headers:h});if(!r.ok){let msg=`요청 처리 실패 (${r.status})`;try{const body=await r.json();if(body?.message)msg=body.message;}catch(_){}throw Error(msg);}if(r.status===204)return null;const body=await r.text();return body?JSON.parse(body):null;}
function text(id,value){const e=$(id);if(e)e.textContent=value||'-';}
function syncInspectionDeleteButton(){
 const button=$('in-detail-delete');if(!button)return;
 const remaining=(comparisonCache.get(String(current?.no))||[]).length;
 button.disabled=remaining>0 || !current?.canDelete;
 button.title=remaining>0?'점검 기록을 삭제하려면 모든 첨부파일을 먼저 삭제해 주세요.':'점검 기록 삭제';
}
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
 selector.dataset.lastCompared='';selector.replaceChildren();const option=document.createElement('option');option.value='';option.textContent='비교할 다른 점검 기록 선택';selector.append(option);
 for(const r of comparisonRecords){const o=document.createElement('option');o.value=String(r.no);o.textContent=`${readableDate(r.inspectionDate)} · ${r.title||'제목 없음'}`;selector.append(o);}
 result.replaceChildren();panel.hidden=false;
 if(!comparisonRecords.length)result.textContent='이 교량에는 비교할 다른 점검 기록이 없습니다.';
}
async function renderComparison(){
 const panel=$('in-comparison-panel'),selector=$('in-compare-record'),result=$('in-compare-results');
 if(!panel||!selector||!result||panel.hidden)return;
 const no=selector.value;const token=++comparisonToken;
 result.replaceChildren();
 result.setAttribute('aria-busy',no?'true':'false');
 if(!no){result.textContent='기록을 선택하면 해당 기록에 등록된 사진과 영상을 확인할 수 있습니다.';return;}
 const record=comparisonRecords.find(r=>String(r.no)===String(no));
 const heading=document.createElement('p');heading.className='in-compare-label';
 heading.textContent=`비교 대상: ${record?.title||'점검 기록'} (${readableDate(record?.inspectionDate)})`;
 result.append(heading);
 const status=document.createElement('p');status.textContent='첨부 자료를 불러오는 중...';result.append(status);
 // 선택 결과가 화면 아래에 있어도 즉시 확인할 수 있도록 결과 영역으로 이동합니다.
 result.scrollIntoView({behavior:'smooth',block:'nearest'});
 try{
  // 서버 응답 데이터가 아닌 이전 선택 캐시 때문에 보이지 않는 현상을 피하도록 매번 새로 조회합니다.
  const media=uniqueMedia(await json(`/api/media/INSPECTION/${encodeURIComponent(no)}`));
  if(token!==comparisonToken || selector.value!==no)return;
  comparisonCache.set(String(no),media);
  status.remove();
  const summary=document.createElement('p');summary.className='in-compare-summary';
  const photos=media.filter(m=>mediaKind(m)==='IMAGE');
  const videos=media.filter(m=>mediaKind(m)==='VIDEO');
  summary.textContent=`현장 증거 사진 ${photos.length}개 · 드론 영상 ${videos.length}개`;
  result.append(summary);
  if(!media.length){const empty=document.createElement('p');empty.textContent='이 기록에 등록된 사진이나 영상이 없습니다.';result.append(empty);return;}
  const selectedMedia=comparisonCache.get(String(current?.no))||[];
  const fingerprints=existingSignatures(selectedMedia);
  for(const [label,files] of [['현장 증거 사진',photos],['드론 영상',videos]]){
   const heading=document.createElement('h4');heading.textContent=`${label} (${files.length})`;result.append(heading);
   if(!files.length){const empty=document.createElement('p');empty.textContent='등록된 자료가 없습니다.';result.append(empty);continue;}
   const grid=document.createElement('div');grid.className='in-comparison-grid';
   files.forEach(m=>grid.append(compareCard(m,fingerprints)));result.append(grid);
  }
 }catch(e){
  if(token!==comparisonToken || selector.value!==no)return;
  status.textContent=`비교 자료를 불러오지 못했습니다: ${e.message}`;
  status.setAttribute('role','alert');
 }finally{if(token===comparisonToken)result.setAttribute('aria-busy','false');}
}

function addMedia(m,target){
 const card=document.createElement('div');card.className='in-photo-entry';
 const a=document.createElement('a');a.className='in-photo-card';a.href=mediaUrl(m.no);a.target='_blank';a.rel='noopener';
 const img=document.createElement('img');img.src=a.href;img.alt=m.originName||'점검 사진';img.loading='lazy';
 const name=document.createElement('span');name.textContent=m.originName||`사진 #${m.no}`;
 a.append(img,name);card.append(a);
 // 서버가 계산한 삭제 권한이 있는 첨부파일에만 삭제 버튼을 표시합니다.
 if(m.canDelete){
  const remove=document.createElement('button');remove.type='button';remove.className='in-media-delete in-photo-delete';remove.textContent='사진 삭제';
  remove.setAttribute('aria-label',`${m.originName||'현장 증거 사진'} 삭제`);
  remove.addEventListener('click',async()=>{
   if(!confirm(`사진 '${m.originName||m.no}'을(를) 삭제하시겠습니까? 삭제 후 복구할 수 없습니다.`))return;
   remove.disabled=true;
   try{
    await json(`/api/media/${encodeURIComponent(m.no)}`,{method:'DELETE'});
    card.remove();comparisonCache.set(String(current?.no),(comparisonCache.get(String(current?.no))||[]).filter(x=>x.no!==m.no));
    if(!target.querySelector('.in-photo-entry'))target.textContent='등록된 현장 사진이 없습니다.';
    syncInspectionDeleteButton();
    text('in-detail-message','사진이 삭제되었습니다.');
   }catch(e){text('in-detail-message',`사진 삭제 실패: ${e.message}`);remove.disabled=false;}
  });
  card.append(remove);
 }
 target.append(card);
}
async function select(no){const selectedToken=++selectionToken;comparisonToken++;const panel=$('in-record-detail');panel.hidden=false;text('in-detail-message','기록 조회 중...');document.querySelectorAll('.in-record-row').forEach(x=>x.classList.toggle('is-selected',x.dataset.recordNo===String(no)));try{const all=await json(`/api/inspections/bridge/${encodeURIComponent(bridge)}`);const found=all.find(r=>String(r.no)===String(no));if(!found)throw Error('선택한 점검 기록을 찾을 수 없습니다.');const record=await json(`/api/inspections/${encodeURIComponent(found.id)}`);if(selectedToken!==selectionToken)return;current=record;const recordDelete=$('in-detail-delete');if(recordDelete){recordDelete.hidden=!record.canDelete;recordDelete.disabled=true;}text('detail-title',record.title);text('detail-type',record.inspectionType);text('detail-date',record.inspectionDate);text('detail-status',record.status);text('detail-inspector',String(record.inspectorNo||''));text('detail-overall-result',record.overallResult);const opinion=$('detail-review-opinion');if(opinion){const available=Object.prototype.hasOwnProperty.call(record,'reviewOpinion');opinion.textContent=available?(record.reviewOpinion?.trim()||'-'):'-';opinion.classList.toggle('in-data-unavailable',!available);}text('detail-memo',record.memo);$('in-detail-edit').href=`/inspections/new?id=${encodeURIComponent(record.id)}`;history.replaceState(null,'',`/inspections/${bridge}?inspectionId=${encodeURIComponent(record.id)}`);
const media=uniqueMedia(await json(`/api/media/INSPECTION/${encodeURIComponent(no)}`));if(selectedToken!==selectionToken)return;comparisonCache.set(String(no),media);resetComparison(all,no);const gallery=$('in-detail-photos'),videoList=$('in-detail-videos'),player=$('in-detail-player');gallery.replaceChildren();videoList.replaceChildren();player.pause();player.removeAttribute('src');player.load();player.hidden=true;$('in-player-empty').hidden=false;const images=media.filter(m=>mediaKind(m)==='IMAGE');if(!images.length)gallery.textContent='등록된 현장 사진이 없습니다.';else images.forEach(m=>addMedia(m,gallery));const actualVideos=media.filter(m=>mediaKind(m)==='VIDEO');if(!actualVideos.length){const li=document.createElement('li');li.textContent='등록된 드론 영상이 없습니다.';videoList.append(li);}for(const v of actualVideos){const li=document.createElement('li'),entry=document.createElement('div'),button=document.createElement('button');entry.className='in-video-entry';button.type='button';button.className='in-video-choice';button.textContent=v.originName||`영상 #${v.no}`;button.addEventListener('click',()=>{player.src=mediaUrl(v.no);player.hidden=false;$('in-player-empty').hidden=true;player.load();});entry.append(button);const remove=document.createElement('button');remove.type='button';remove.className='in-media-delete';remove.textContent='삭제';remove.setAttribute('aria-label',`${v.originName||'드론 영상'} 삭제`);remove.addEventListener('click',async()=>{if(!confirm(`영상 '${v.originName||v.no}'을(를) 삭제하시겠습니까? 삭제 후 복구할 수 없습니다.`))return;remove.disabled=true;try{await json(`/api/media/${encodeURIComponent(v.no)}`,{method:'DELETE'});if(player.src.includes(`/api/media/file/${v.no}`)){player.pause();player.removeAttribute('src');player.load();player.hidden=true;$('in-player-empty').hidden=false;}li.remove();comparisonCache.set(String(current?.no),(comparisonCache.get(String(current?.no))||[]).filter(x=>x.no!==v.no));if(!videoList.children.length){const empty=document.createElement('li');empty.textContent='등록된 드론 영상이 없습니다.';videoList.append(empty);}syncInspectionDeleteButton();text('in-detail-message','영상이 삭제되었습니다.');}catch(e){text('in-detail-message',`영상 삭제 실패: ${e.message}`);remove.disabled=false;}});if(v.canDelete)entry.append(remove);li.append(entry);videoList.append(li);}syncInspectionDeleteButton();text('in-detail-message','점검 기록 및 첨부 자료');panel.scrollIntoView({behavior:'smooth',block:'start'});}catch(e){text('in-detail-message',e.message);}}
// 키보드/마우스 선택 모두 반영하고, 선택 이벤트가 겹칠 때는 마지막 요청만 표시합니다.
const comparisonSelector=$('in-compare-record');
if(comparisonSelector){
 comparisonSelector.addEventListener('change',()=>{if(comparisonSelector.value===comparisonSelector.dataset.lastCompared)return;comparisonSelector.dataset.lastCompared=comparisonSelector.value;renderComparison();});
 comparisonSelector.addEventListener('input',()=>{
  if(comparisonSelector.value!==comparisonSelector.dataset.lastCompared){
   comparisonSelector.dataset.lastCompared=comparisonSelector.value;
   renderComparison();
  }
 });
}
document.querySelectorAll('.in-record-row').forEach(b=>b.addEventListener('click',()=>select(b.dataset.recordNo)));
$('in-detail-delete')?.addEventListener('click',async()=>{if(!current?.canDelete||!confirm(`'${current.title}' 점검 기록을 삭제하시겠습니까? 첨부파일이 있으면 먼저 삭제해야 합니다.`))return;try{await json(`/api/inspections/${encodeURIComponent(current.id)}`,{method:'DELETE'});location.assign('/inspections');}catch(e){const m=$('in-delete-message');m.textContent=`삭제 실패: ${e.message}. 첨부파일이 남아 있으면 먼저 삭제하고, 다른 담당자의 기록은 기업 관리자에게 요청해 주세요.`;m.hidden=false;}});
(async()=>{const id=new URLSearchParams(location.search).get('inspectionId');if(!id)return;try{const list=await json(`/api/inspections/bridge/${encodeURIComponent(bridge)}`);const r=list.find(x=>x.id===id);if(r)await select(r.no);}catch(e){text('in-detail-message',e.message);}})();
})();
