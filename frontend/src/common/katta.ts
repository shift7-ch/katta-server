
export function isAwsHostname(hostname: string, cn: boolean = true): boolean {
    var match = (cn ? hostname.match(/^([a-z0-9\-]+\.)?s3(\.dualstack)?(\.[a-z0-9\-]+)?(\.vpce)?\.amazonaws\.com(\.cn)?$/) : hostname.match(/^([a-z0-9\-]+\.)?s3(\.dualstack)?(\.[a-z0-9\-]+)?(\.vpce)?\.amazonaws\.com$/))
    return match != null;
}
