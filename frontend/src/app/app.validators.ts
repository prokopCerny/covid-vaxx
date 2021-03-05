import {AbstractControl, ValidationErrors} from '@angular/forms';


const personalNumberAddingTwentyIssueYear = 4;
const tenDigitPersonalNumberIssueYear = 54;
const womanMonthAddition = 50;
const unprobableMonthAddition = 20;

/**
 * Validates phone number in format +123456789123.
 */
export function validatePhoneNumber(control: AbstractControl): ValidationErrors | null {
  const regex = /^\+\d{12}$/;
  if (!regex.test(control.value)) {
    return {phoneNumberInvalid: true};
  }
  return null;
}

/**
 * Validates email.
 */
export function validateEmail(control: AbstractControl): ValidationErrors | null {
  const regex = /(?:[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])/;
  if (!regex.test(control.value)) {
    return {emailInvalid: true};
  }
  return null;
}

/**
 * Validates personal number.
 */
export function validatePersonalNumber(control: AbstractControl): ValidationErrors | null {
  if (!control.value) {
    return {personalNumberInvalid: true};
  }

  let firstPart = '';
  let secondPart = '';

  const parts = control.value.split('/');
  if (parts.length === 1) {
    firstPart = control.value.substr(0, 6);
    secondPart = control.value.substr(6);
  } else {
    firstPart = parts[0];
    secondPart = parts[1];
  }

  if (firstPart.length !== 6 || isNaN(Number(firstPart)) || isNaN(Number(secondPart))) {
    return {personalNumberInvalid: true};
  }

  const year = Number(firstPart.substr(0, 2));
  let month = Number(firstPart.substr(2, 2));
  const day = Number(firstPart.substr(4, 2));

  const currentYear = (new Date()).getFullYear() % 100;

  if (year >= tenDigitPersonalNumberIssueYear || year <= currentYear) {
    if (secondPart.length === 4) {
      const controlDigit = Number(secondPart.substr(3, 1));
      const concatenated = Number(firstPart + secondPart);

      const moduloElevenOk = concatenated % 11 === 0;
      const withoutLastDigit = concatenated / 10;
      const moduloTenOk = (withoutLastDigit % 11) === 10 && controlDigit === 0;

      if (!moduloTenOk && !moduloElevenOk) {
        return {personalNumberInvalid: true};
      }
    } else {
      return {personalNumberInvalid: true};
    }
  } else {
    if (secondPart.length !== 3) {
      return {personalNumberInvalid: true};
    }
  }
  if (month > womanMonthAddition) {
    month -= womanMonthAddition;
  }

  if (month > unprobableMonthAddition) {
    if (year >= personalNumberAddingTwentyIssueYear) {
      month -= unprobableMonthAddition;
    } else {
      return {personalNumberInvalid: true};
    }
  }

  if (!isDateValid(year, month, day)) {
    return {personalNumberInvalid: true};
  }
  return null;
}

/**
 * Validates if date parts can form valid date.
 */
function isDateValid(year: number, month: number, day: number): boolean {
  const fullYear = year >= tenDigitPersonalNumberIssueYear ? 1900 + year : 2000 + year;
  try {
    /* tslint:disable:no-unused-expression */
    new Date(fullYear, month, day);
    return true;
  } catch {
    return false;
  }
}
