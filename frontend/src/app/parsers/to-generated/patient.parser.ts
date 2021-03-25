import {AnswerDtoOut, PatientRegistrationDtoIn, PatientUpdateDtoIn, PhoneNumberDtoIn} from '../../generated';
import {fromQuestionToAnswerGenerated} from './question.parser';
import {fromInsuranceToInsuranceGenerated, fromInsuranceToUpdateInsuranceGenerated} from '@app/parsers/to-generated/insurance.parse';
import {PatientData} from '@app/model/PatientData';
import {ParsedNumber, parseNumber} from 'libphonenumber-js';

export const fromPatientToRegistrationGenerated = (patient: PatientData, agreement: boolean, confirmation: boolean, gdpr: boolean): PatientRegistrationDtoIn => {
  return {
    ...fromPatientToPartialGenerated(patient),
    insuranceCompany: fromInsuranceToInsuranceGenerated(patient.insuranceCompany),
    confirmation: {
      covid19VaccinationAgreement: agreement,
      healthStateDisclosureConfirmation: confirmation,
      gdprAgreement: gdpr
    }
  };
};

export const fromPatientToUpdateGenerated = (patient: PatientData): PatientUpdateDtoIn => {
  return {
    ...fromPatientToPartialGenerated(patient),
    insuranceCompany: fromInsuranceToUpdateInsuranceGenerated(patient.insuranceCompany)
  };
};

const fromPatientToPartialGenerated = (patient: PatientData): PatientPartialDataGenerated => {
  return {
    firstName: patient.firstName.trim(),
    lastName: patient.lastName.trim(),
    personalNumber: patient.personalNumber.trim(),
    email: patient.email.trim(),
    phoneNumber: parsePhoneNumber(patient.phoneNumber.trim()),
    zipCode: +patient.zipCode.replace(' ', ''),
    district: patient.district.trim(),
    answers: patient.questionnaire.map(fromQuestionToAnswerGenerated)
  };
};

const parsePhoneNumber = (value: string): PhoneNumberDtoIn => {
  const parsed: ParsedNumber = parseNumber(value) as ParsedNumber;

  // TODO
  return {
    countryCode: '',
    number: ''
  };
};

interface PatientPartialDataGenerated {
  firstName: string;
  lastName: string;
  personalNumber: string;
  email: string;
  phoneNumber: PhoneNumberDtoIn;
  district: string;
  zipCode: number;
  answers: AnswerDtoOut[];
}
