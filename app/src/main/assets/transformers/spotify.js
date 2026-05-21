/*
 * Argus Ad Blocker — Spotify API Response Transformer
 * @host *.spotify.com
 * @path /bootstrap,/*/pendragon/*,/user-customization-service/*
 * @content-type application/json,text/json
 */

function transform(url, host, path, contentType, body) {
    // Block pendragon ad endpoint entirely
    if (path.indexOf('/pendragon/') >= 0) {
        return '{}';
    }

    if (!body || body.trim().charAt(0) !== '{') {
        return body;
    }

    try {
        var json = JSON.parse(body);
        var modified = false;

        if (json.ads !== undefined) { delete json.ads; modified = true; }
        if (json.ad_formats !== undefined) { delete json.ad_formats; modified = true; }
        if (json.sponsorships !== undefined) { delete json.sponsorships; modified = true; }

        // Disable ad-related user attributes
        if (json.attributes && typeof json.attributes === 'object') {
            var adAttrs = ['ads_enabled', 'show_ads', 'ad_supported'];
            for (var i = 0; i < adAttrs.length; i++) {
                if (json.attributes[adAttrs[i]] !== undefined) {
                    json.attributes[adAttrs[i]] = false;
                    modified = true;
                }
            }
        }

        return modified ? JSON.stringify(json) : body;
    } catch (e) {
        return body;
    }
}
