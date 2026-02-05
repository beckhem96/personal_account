import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(amount);
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat('ko-KR').format(value);
}

/**
 * 쉼표가 포함된 수식 문자열을 계산한다.
 * 허용: 숫자, 쉼표(천단위 구분), +, -, *, /, (), 소수점, 공백
 */
export function evaluateExpr(expr: string): number | null {
  // 쉼표 제거 (천단위 구분자)
  const cleaned = expr.replace(/,/g, '').replace(/\s/g, '');
  if (!cleaned) return null;
  if (!/^[\d+\-*/().]+$/.test(cleaned)) return null;
  try {
    const result = new Function(`return (${cleaned})`)() as number;
    if (typeof result === 'number' && isFinite(result)) return result;
    return null;
  } catch {
    return null;
  }
}

/**
 * 입력 중인 수식 문자열의 숫자 부분에 쉼표를 자동 삽입한다.
 * "2000 + 3000" → "2,000 + 3,000"
 */
export function formatExpr(expr: string): string {
  return expr.replace(/\d[\d,]*/g, (match) => {
    const num = match.replace(/,/g, '');
    if (!/^\d+$/.test(num)) return match;
    return new Intl.NumberFormat('ko-KR').format(Number(num));
  });
}
