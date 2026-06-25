import { describe, it, expect } from 'vitest';
import { evaluateExpr, formatExpr } from './index';

describe('evaluateExpr 수식 계산기 테스트', () => {
  it('기본 사칙연산 수식을 계산할 수 있다', () => {
    expect(evaluateExpr('15000+4500')).toBe(19500);
    expect(evaluateExpr('10000 - 3000')).toBe(7000);
    expect(evaluateExpr('2000 * 3')).toBe(6000);
    expect(evaluateExpr('10000 / 4')).toBe(2500);
  });

  it('괄호 및 혼합 사칙연산을 계산할 수 있다', () => {
    expect(evaluateExpr('(5000 + 3000) * 2')).toBe(16000);
    expect(evaluateExpr('10000 + 2000 * 3')).toBe(16000);
  });

  it('쉼표(천단위 구분자)가 포함된 수식도 계산할 수 있다', () => {
    expect(evaluateExpr('15,000 + 4,500')).toBe(19500);
    expect(evaluateExpr('(1,000,000 - 500,000) / 2')).toBe(250000);
  });

  it('유효하지 않은 문자나 잘못된 수식은 null을 반환한다', () => {
    expect(evaluateExpr('15000 + abc')).toBeNull();
    expect(evaluateExpr('15000++4500')).toBeNull();
    expect(evaluateExpr('15000 / 0')).toBeNull(); // Infinity 방지
    expect(evaluateExpr('')).toBeNull();
  });
});

describe('formatExpr 쉼표 자동 삽입 테스트', () => {
  it('수식 내 개별 숫자에 대해 쉼표를 자동 포맷팅한다', () => {
    expect(formatExpr('15000 + 4500')).toBe('15,000 + 4,500');
    expect(formatExpr('(1000000 - 500000) / 2')).toBe('(1,000,000 - 500,000) / 2');
  });

  it('쉼표가 이미 있거나 소수점 등이 있어도 올바르게 유지 또는 대체한다', () => {
    expect(formatExpr('1,5000 + 4500')).toBe('15,000 + 4,500');
  });
});
