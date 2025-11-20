// src/utils/ip.ts

/**
 * IP 주소 가져오기
 */
async function getClientIp(): Promise<string | null> {
  const ipAddress = await fetch('https://api.ipify.org?format=json');
  const data = await ipAddress.json();
  return data.ip || null;
}

export { getClientIp };
