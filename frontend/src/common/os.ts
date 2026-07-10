// / start katta extension
export function detectOS(): 'mac' | 'win' | 'other' {
  if (typeof navigator === 'undefined') {
    return 'other';
  }
  const userAgent = navigator.userAgent.toLowerCase();
  // iPhones report "like mac os x" and iPadOS desktop mode reports "Macintosh"; treat all touch/mobile devices as 'other'.
  if (/iphone|ipad|ipod|android|mobile/.test(userAgent) || (/macintosh/.test(userAgent) && navigator.maxTouchPoints > 1)) {
    return 'other';
  }
  if (/windows/.test(userAgent)) {
    return 'win';
  }
  if (/macintosh|mac os x/.test(userAgent)) {
    return 'mac';
  }
  return 'other';
}
// \ end katta extension
