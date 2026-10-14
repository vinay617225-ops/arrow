"""Independent grid-rule oracle; this does NOT execute Kotlin or Android code."""
import random
from pathlib import Path
import xml.etree.ElementTree as ET
rng=random.Random(20261014)
count=0
for _ in range(20000):
 n=7
 body=[(rng.randrange(n),rng.randrange(n))]
 for step in range(rng.randrange(1,9)):
  x,y=body[-1];dx,dy=rng.choice([(1,0),(-1,0),(0,1),(0,-1)])
  nxt=x+dx,y+dy
  if nxt not in body and 0<=nxt[0]<n and 0<=nxt[1]<n: body.append(nxt)
 if len(body)<2: continue
 dx=body[-1][0]-body[-2][0];dy=body[-1][1]-body[-2][1]
 blockers={(rng.randrange(n),rng.randrange(n)) for _ in range(12)}-set(body)
 x,y=body[-1];ray=[]
 while True:
  x+=dx;y+=dy
  if not(0<=x<n and 0<=y<n): break
  ray.append((x,y))
 ray_blocked=bool(set(ray)&blockers)
 moving=body[:];swept=False
 for step in range(2*n+len(body)):
  x,y=moving[-1];moving=moving[1:]+[(x+dx,y+dy)]
  swept |= bool(set(moving)&blockers)
 assert swept==ray_blocked
 assert all(not(0<=x<n and 0<=y<n) for x,y in moving)
 count+=1
ET.parse(Path(__file__).resolve().parents[1] / 'app/src/main/AndroidManifest.xml')
print(f'Independent grid-rule oracle: {count} randomized ray/full-body equivalence cases passed.')
print('Android manifest XML parses. This Python check does not execute Kotlin or Android tests.')
