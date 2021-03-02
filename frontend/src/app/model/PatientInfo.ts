export class PatientInfo {
  firstName: string;
  lastName: string;
  birthNumber: number;
  insuranceCompany: string;
  phone: string;
  email: string;

  questions: YesNoQuestion[] = [];

  constructor() {
    this.questions.push(...[
      { label: 'Cítíte se v tuto chvílí nemocný/á', name: 'is_sick' },
      { label: 'Prodělal/a jste onemocnění COVID-19 nebo jste měl/a pozitivní PCR test?', name: 'had_covid' },
      { label: 'Byl/a jste již očkován/a proti nemoci COVID-19', name: 'is_vaccinated_against_covid' },
      { label: 'Měl/a jste někdy závažnou alergickou reakci po očkování?', name: 'had_allergic_reaction' },
      { label: 'Máte nějakou krvácivou poruchu nebo berete léky na ředění krve?', name: 'has_blood_problems' },
      { label: 'Máte nějakou zývažnou poruchu imunity?', name: 'has_immunity_problems' },
      { label: 'Jste těhotná nebo kojíte?', name: 'is_pregnant' },
      { label: 'Absolvovala jste v posledních dvou týdnech nějaké jiné očkování?', name: 'is_vaccinated_in_last_two_weeks' },
    ]);
  }
}

export interface YesNoQuestion {
  label: string;
  name: string;
  value?: boolean;
}
