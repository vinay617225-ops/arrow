#!/usr/bin/env bash
# Use writable caches and the configured proxy in a managed cloud workspace.
set -euo pipefail
project_root="$(cd -- "$(dirname -- "$0")/.." && pwd)"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$project_root/work/gradle-home}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$project_root/work/android-user}"
mkdir -p "$GRADLE_USER_HOME" "$ANDROID_USER_HOME" "$project_root/work/java-user"
if [[ -x "$project_root/work/jdk/bin/javac" && -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME="$project_root/work/jdk"
fi
proxy_args=()
if [[ -n "${HTTPS_PROXY:-${https_proxy:-}}" ]]; then
  read -r proxy_host proxy_port < <(python - <<'PY'
import os, urllib.parse
proxy=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy'))
print(proxy.hostname, proxy.port or (443 if proxy.scheme=='https' else 80))
PY
  )
  proxy_args=(-Dhttp.proxyHost="$proxy_host" -Dhttp.proxyPort="$proxy_port" -Dhttps.proxyHost="$proxy_host" -Dhttps.proxyPort="$proxy_port")
fi
trust_args=()
if [[ -f /etc/ssl/certs/java/cacerts ]]; then
  trust_args=(-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts)
fi
cd "$project_root"
exec ./gradlew --no-daemon --max-workers=3 -Duser.home="$project_root/work/java-user" "${proxy_args[@]}" "${trust_args[@]}" -Pkotlin.compiler.execution.strategy=in-process "$@"
