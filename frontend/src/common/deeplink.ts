// / start katta extension
import { baseURL } from './config';

export function openInKatta(): void {
  window.location.href = `katta://${location.host}${baseURL}`;
}
// \ end katta extension
