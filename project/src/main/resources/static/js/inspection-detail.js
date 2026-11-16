(() => {
'use strict';const main=document.querySelector('#main-content');if(!main)return;const bridge=main.dataset.bridgeNo,$=id=>document.getElementById(id);let current=null;const csrf=document.querySelector('meta[name="_csrf"]')?.content, csrfHeader=document.querySelector('meta[name="_csrf_header"]')?.content;
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
    card.remove();
    if(!target.querySelector('.in-photo-entry'))target.textContent='등록된 현장 사진이 없습니다.';
    text('in-detail-message','사진이 삭제되었습니다.');
   }catch(e){text('in-detail-message',`사진 삭제 실패: ${e.message}`);remove.disabled=false;}
  });
  card.append(remove);
 }
 target.append(card);
}
async function select(no){const panel=$('in-record-detail');panel.hidden=false;text('in-detail-message','기록 조회 중...');document.querySelectorAll('.in-record-row').forEach(x=>x.classList.toggle('is-selected',x.dataset.recordNo===String(no)));try{const all=await json(`/api/inspections/bridge/${encodeURIComponent(bridge)}`);const found=all.find(r=>String(r.no)===String(no));if(!found)throw Error('선택한 점검 기록을 찾을 수 없습니다.');const record=await json(`/api/inspections/${encodeURIComponent(found.id)}`);current=record;text('detail-title',record.title);text('detail-type',record.inspectionType);text('detail-date',record.inspectionDate);text('detail-status',record.status);text('detail-inspector',String(record.inspectorNo||''));text('detail-overall-result',record.overallResult);const opinion=$('detail-review-opinion');if(opinion){const available=Object.prototype.hasOwnProperty.call(record,'reviewOpinion');opinion.textContent=available?(record.reviewOpinion||'입력된 검토 의견이 없습니다.'):'현재 조회 API에서 검토 의견을 제공하지 않습니다.';opinion.classList.toggle('in-data-unavailable',!available);}text('detail-memo',record.memo);$('in-detail-edit').href=`/inspections/new?id=${encodeURIComponent(record.id)}`;history.replaceState(null,'',`/inspections/${bridge}?inspectionId=${encodeURIComponent(record.id)}`);
const media=uniqueMedia(await json(`/api/media/INSPECTION/${encodeURIComponent(no)}`));const gallery=$('in-detail-photos'),videoList=$('in-detail-videos'),player=$('in-detail-player');gallery.replaceChildren();videoList.replaceChildren();player.pause();player.removeAttribute('src');player.load();player.hidden=true;$('in-player-empty').hidden=false;const images=media.filter(m=>mediaKind(m)==='IMAGE');if(!images.length)gallery.textContent='등록된 현장 사진이 없습니다.';else images.forEach(m=>addMedia(m,gallery));const actualVideos=media.filter(m=>mediaKind(m)==='VIDEO');if(!actualVideos.length){const li=document.createElement('li');li.textContent='등록된 드론 영상이 없습니다.';videoList.append(li);}for(const v of actualVideos){const li=document.createElement('li'),entry=document.createElement('div'),button=document.createElement('button');entry.className='in-video-entry';button.type='button';button.className='in-video-choice';button.textContent=v.originName||`영상 #${v.no}`;button.addEventListener('click',()=>{player.src=mediaUrl(v.no);player.hidden=false;$('in-player-empty').hidden=true;player.load();});entry.append(button);const remove=document.createElement('button');remove.type='button';remove.className='in-media-delete';remove.textContent='삭제';remove.setAttribute('aria-label',`${v.originName||'드론 영상'} 삭제`);remove.addEventListener('click',async()=>{if(!confirm(`영상 '${v.originName||v.no}'을(를) 삭제하시겠습니까? 삭제 후 복구할 수 없습니다.`))return;remove.disabled=true;try{await json(`/api/media/${encodeURIComponent(v.no)}`,{method:'DELETE'});if(player.src.includes(`/api/media/file/${v.no}`)){player.pause();player.removeAttribute('src');player.load();player.hidden=true;$('in-player-empty').hidden=false;}li.remove();if(!videoList.children.length){const empty=document.createElement('li');empty.textContent='등록된 드론 영상이 없습니다.';videoList.append(empty);}text('in-detail-message','영상이 삭제되었습니다.');}catch(e){text('in-detail-message',`영상 삭제 실패: ${e.message}`);remove.disabled=false;}});entry.append(remove);li.append(entry);videoList.append(li);}text('in-detail-message','점검 기록 및 첨부 자료');panel.scrollIntoView({behavior:'smooth',block:'start'});}catch(e){text('in-detail-message',e.message);}}
document.querySelectorAll('.in-record-row').forEach(b=>b.addEventListener('click',()=>select(b.dataset.recordNo)));
$('in-detail-delete')?.addEventListener('click',async()=>{if(!current||!confirm(`'${current.title}' 점검 기록을 삭제하시겠습니까? 첨부파일이 있으면 먼저 삭제해야 합니다.`))return;try{await json(`/api/inspections/${encodeURIComponent(current.id)}`,{method:'DELETE'});location.assign('/inspections');}catch(e){const m=$('in-delete-message');m.textContent=`삭제 실패: ${e.message}. 첨부파일이 있다면 관리자 권한으로 먼저 정리해야 합니다.`;m.hidden=false;}});
(async()=>{const id=new URLSearchParams(location.search).get('inspectionId');if(!id)return;try{const list=await json(`/api/inspections/bridge/${encodeURIComponent(bridge)}`);const r=list.find(x=>x.id===id);if(r)await select(r.no);}catch(e){text('in-detail-message',e.message);}})();
})();
