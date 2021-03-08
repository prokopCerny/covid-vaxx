import { Patient } from '../model/Patient';
import { InsuranceCompany } from '../model/InsuranceCompany';
import { AnswerDto, PatientDtoOut } from '@app/generated';
import { Answer } from '@app/model/Answer';
import { Question } from '@app/model/Question';

export const parsePatient = (data: PatientDtoOut, questions: Question[]): Patient => {
  const answers = data.answers.map(a => parseAnswer(a, questions));

  return {
    ...data,
    answers: answers.filter(notEmpty),
    insuranceCompany: parseInsuranceCompany(data.insuranceCompany),
    created: new Date(data.created),
    updated: new Date(data.updated)
  };
};

export const parseInsuranceCompany = (data: string): InsuranceCompany | undefined => {
  return InsuranceCompany[data as InsuranceCompany];
};

export const parseAnswer = (data: AnswerDto, questions: Question[]): Answer | undefined => {
  const question = questions.find(q => q.id === data.questionId);
  if (!question) {
    return undefined;
  }

  return {
    label: question.label,
    value: data.value
  };
};

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function notEmpty<TValue>(value: TValue | undefined): value is TValue {
  return value !== undefined;
}