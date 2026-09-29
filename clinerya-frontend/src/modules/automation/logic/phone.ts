/** Celular de Mexico tal como lo reporta WhatsApp: 52 + 1 + 10 digitos. */
function isMexicanMobile(digits: string): boolean {
  return digits.length === 13 && digits.startsWith("521");
}

/** Numero completo y legible, para que el medico vea el suyo: "+52 1 55 9999 0000". */
export function formatPhone(phone: string | null): string {
  if (!phone) {
    return "";
  }
  const digits = phone.replace(/\D/g, "");
  if (isMexicanMobile(digits)) {
    return `+52 1 ${digits.slice(3, 5)} ${digits.slice(5, 9)} ${digits.slice(9)}`;
  }
  return `+${digits}`;
}
