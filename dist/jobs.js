import {analyzeVin} from './vin.js';
export const freshJob=()=>({id:'job-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2,7),job:'',vin:'',vehicle:'',rows:[],createdAt:new Date().toISOString(),status:'Open'});
export function changeVehicle(job,vin,vehicle=''){
 const next=structuredClone(job);const changed=String(job.vin).trim().toUpperCase()!==String(vin).trim().toUpperCase();
 next.vin=vin;next.vehicle=vehicle;
 if(changed){delete next.vehicleDetails;next.rows=next.rows.map(r=>({...r,selection:r.selection?{...r.selection,stale:true}:undefined}));}
 return next;
}
export function attachSelection(job,rowId,result){
 if(!analyzeVin(job.vin).valid||result.vin!==analyzeVin(job.vin).vin)throw Error('The job VIN changed. Run the lookup again for this vehicle.');
 if(!/^[A-Z0-9][A-Z0-9 -]{2,39}$/.test(result.serviceNumber||''))throw Error('Enter a valid complete service number.');
 const next=structuredClone(job),r=next.rows.find(r=>r.id===rowId);
 if(!r||!r.bases.includes(result.base))throw Error('The selected base is not part of this job item.');
 r.selection={...result,stale:false,selectedAt:new Date().toISOString()};return next;
}
export function migrateReferences(parts,favorites,notes){
 const current=new Set(parts.map(p=>p.id)),map=new Map(parts.flatMap(p=>(p.legacyIds||[]).filter(id=>!current.has(id)).map(id=>[id,p.id])));
 const ids=[...new Set(favorites.map(id=>map.get(id)||id))],out={};
 for(const [id,note]of Object.entries(notes)){const key=map.get(id)||id;out[key]=[out[key],note].filter(Boolean).join('\n\n');}
 return {favorites:ids,notes:out};
}
export function validateJobs(jobs){
 if(!Array.isArray(jobs)||jobs.length>500)throw Error('Choose a backup with at most 500 jobs.');
 const ids=new Set();
 for(const j of jobs){
  if(!j||typeof j.id!=='string'||ids.has(j.id)||typeof j.job!=='string'||typeof j.vin!=='string'||(j.vehicle!==undefined&&typeof j.vehicle!=='string')||!Array.isArray(j.rows)||j.rows.length>2000)throw Error('Invalid job in backup.');ids.add(j.id);
  for(const r of j.rows){if(typeof r.id!=='string'||typeof r.name!=='string'||!Array.isArray(r.bases)||!r.bases.every(b=>/^\d[A-Z0-9]{3,7}$/.test(b))||!Number.isInteger(r.qty)||r.qty<1||r.qty>999||typeof r.note!=='string')throw Error('Invalid job item in backup.');
   if(r.selection){const s=r.selection;if(typeof s.vin!=='string'||!r.bases.includes(s.base)||typeof s.serviceNumber!=='string'||!s.serviceNumber||s.serviceNumber.length>40||typeof s.source!=='string'||(s.context!==undefined&&typeof s.context!=='string')||(s.description!==undefined&&typeof s.description!=='string'))throw Error('Invalid selected part in backup.');}
  }
 }
 return true;
}
