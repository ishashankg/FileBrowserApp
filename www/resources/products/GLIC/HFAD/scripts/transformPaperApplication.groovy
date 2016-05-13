import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.util.ReflectionUtils
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.stepsoln.core.db.Lookup;
import com.stepsoln.core.db.cases.Case;
import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.core.db.cases.Case.CASE_SOURCE;
import com.stepsoln.core.db.party.Address;
import com.stepsoln.core.eform.EForm;
import com.stepsoln.core.eform.Question;
import com.stepsoln.core.eform.Section;
import com.stepsoln.core.eform.Page;
import com.stepsoln.core.eform.Group;
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.eform.EFormAnswers;
import com.stepsoln.core.eform.jsf.EFormBeanXMLAdapter
import org.slf4j.Logger;
import com.stepsoln.core.db.party.Agent;
import com.stepsoln.core.db.agency.CarrierProducerAppointment;
import com.stepsoln.core.db.security.SecurityUser;
import com.stepsoln.core.db.services.util.EmployeeUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import com.stepsoln.core.rules.coverages.CoveragePlan;
import com.stepsoln.core.rules.coverages.CoverageOption;
import com.stepsoln.core.db.cases.CaseRequirement;
import com.stepsoln.core.db.enums.REQUIREMENT_TYPE;
import com.stepsoln.core.db.cases.CaseRequirement.STATUS;
import com.stepsoln.core.db.cases.CaseRequirement.CASE_REQUIREMENT_TARGET;
import com.stepsoln.core.db.cases.CaseRequirement.SIGNATURE_STATUS;
import com.stepsoln.core.db.enums.PHONE_TYPE

class EFormTransformer
{
	Case currentCase;
	Map<String, EFormAnswers> eforms;
	List<Lookup> lookups;
	Services services;
	Logger LOGGER;
	EForm currentEForm;
	String currentEFormCode;
	List<CoveragePlan> coveragePlanList;
	List<CaseRequirement> allRequirements;
	
	public void postConvertPaperCaseCustomBehavior()
	{
		LOGGER.info("START postConvertPaperCaseCustomBehavior");
		//System.out.println("START resolveSignatureRequirements");
		
		allRequirements = currentCase.getRequirements();
		if (allRequirements != null)
		{
			for (CaseRequirement req : allRequirements)
			{
				//System.out.println("CUR REQ "+req.toString());
				if (REQUIREMENT_TYPE.ESIGNATURE.equals(req.getType()) && CASE_REQUIREMENT_TARGET.PRIMARY_INSURED.equals(req.getTarget()) && "ACK".equals(req.getRequirementCode()))
				{
					if(eforms.HFA_PAPER_APP.ack_sig_insured != null)
					{
						req.setStatus(STATUS.RESOLVED);
						req.setSignatureStatus(SIGNATURE_STATUS.SIGNED);
						req.setSignatureCity(eforms.HFA_PAPER_APP.ack_city);
						String stateCode = (eforms.HFA_PAPER_APP.ack_state==null) ? null :findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.ack_state).code;
						req.setSignatureState(stateCode);
						req.setSignatureDate(DateUtil.convertDate(eforms.HFA_PAPER_APP.ack_date_insured));
						req.setResolvedDate(req.getSignatureDate());
						services.hibernateTemplate.update(req);
						LOGGER.debug("HFA_PAPER_APP.ack_sig_insured!!!!!!!!!!!");
					}
				}else if(REQUIREMENT_TYPE.ESIGNATURE.equals(req.getType()) && CASE_REQUIREMENT_TARGET.POLICY_OWNER.equals(req.getTarget()) && "ACK".equals(req.getRequirementCode()))
				{
					if(eforms.HFA_PAPER_APP.ack_sig_owner != null)
					{
						//ack_sig_owner
						req.setStatus(STATUS.RESOLVED);
						req.setSignatureStatus(SIGNATURE_STATUS.SIGNED);
						req.setSignatureCity(eforms.HFA_PAPER_APP.ack_city);
						String stateCode = (eforms.HFA_PAPER_APP.ack_state==null) ? null :findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.ack_state).code;
						req.setSignatureState(stateCode);
						req.setSignatureDate(DateUtil.convertDate(eforms.HFA_PAPER_APP.ack_date_owner));
						req.setResolvedDate(req.getSignatureDate());
						services.hibernateTemplate.update(req);
						LOGGER.debug("HFA_PAPER_APP.ack_sig_owner!!!!!!!!!!!");
					}
				}	
				else if (REQUIREMENT_TYPE.ESIGNATURE.equals(req.getType()) && CASE_REQUIREMENT_TARGET.PRIMARY_INSURED.equals(req.getTarget()) 
					&& ("HIP".equals(req.getRequirementCode()) || "FHP".equals(req.getRequirementCode()) || "FCRA".equals(req.getRequirementCode())   ))
				{
					if(eforms.HFA_PAPER_APP.hipaa_sig_insured != null)
					{
						req.setStatus(STATUS.RESOLVED);
						req.setSignatureStatus(SIGNATURE_STATUS.SIGNED);
						req.setSignatureCity(eforms.HFA_PAPER_APP.contract_city);
						String stateCode = (eforms.HFA_PAPER_APP.contract_state==null) ? null :findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.contract_state).code;
						req.setSignatureState(stateCode);
						req.setSignatureDate(DateUtil.convertDate(eforms.HFA_PAPER_APP.hipaa_date_insured));
						req.setResolvedDate(req.getSignatureDate());
						services.hibernateTemplate.update(req);
						LOGGER.debug("HFA_PAPER_APP.hipaa_sig_insured!!!!!!!!!!!");
					}
				}else if(REQUIREMENT_TYPE.ESIGNATURE.equals(req.getType()) && CASE_REQUIREMENT_TARGET.POLICY_OWNER.equals(req.getTarget()) && "HIP".equals(req.getRequirementCode()))
				{
					if(eforms.HFA_PAPER_APP.hipaa_sig_owner != null)
					{
						//ack_sig_owner
						req.setStatus(STATUS.RESOLVED);
						req.setSignatureStatus(SIGNATURE_STATUS.SIGNED);
						req.setSignatureCity(eforms.HFA_PAPER_APP.contract_city);
						String stateCode = (eforms.HFA_PAPER_APP.contract_state==null) ? null :findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.contract_state).code;
						req.setSignatureState(stateCode);
						req.setSignatureDate(DateUtil.convertDate(eforms.HFA_PAPER_APP.hipaa_date_owner));
						req.setResolvedDate(req.getSignatureDate());
						services.hibernateTemplate.update(req);
						LOGGER.debug("HFA_PAPER_APP.hipaa_sig_owner!!!!!!!!!!!");
					}
				}else if(REQUIREMENT_TYPE.ESIGNATURE.equals(req.getType()) && CASE_REQUIREMENT_TARGET.AGENT.equals(req.getTarget()) && "AAT".equals(req.getRequirementCode()))
				{
					
					if(eforms.AGENT_ATTESTATION != null && eforms.HFA_PAPER_APP.agent_sig!=null)
					{
						//agent signature
						req.setStatus(STATUS.RESOLVED);
						req.setSignatureStatus(SIGNATURE_STATUS.SIGNED);
						req.setSignatureDate(DateUtil.convertDate(eforms.HFA_PAPER_APP.agent_date));
						req.setResolvedDate(req.getSignatureDate());
						services.hibernateTemplate.update(req);
						LOGGER.debug("Agent signature resolved");
					}
				}
			}
		}
		
		LOGGER.info("END postConvertPaperCaseCustomBehavior");
		
	}

	public void transform()
	{
		def answerMedicalN = true;
		def answerSmokerN = true;
		LOGGER.debug("Started EForm conversion");
		String delim = EFormBeanXMLAdapter.repeatSectionDelim;
		//Create Applicant and Address
		parsePrimaryApplicant();
		
		setUpApplicationQuoteItems();
		
		//Parse PERSON
		eforms.PERSON.place_of_birth_state = (eforms.HFA_PAPER_APP.place_of_birth_state==null) ? 
													null :findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.place_of_birth_state).code;
		eforms.PERSON.place_of_birth_country = (eforms.HFA_PAPER_APP.place_of_birth_country==null) ? 
													null :findInLookup("LOCALE_COUNTRY", eforms.HFA_PAPER_APP.place_of_birth_country).code;
		eforms.PERSON.driv_lic_number = eforms.HFA_PAPER_APP.driv_lic_number;
		eforms.PERSON.driv_lic_state = (eforms.HFA_PAPER_APP.driv_lic_state==null) ? 
													null :findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.driv_lic_state).code;
		if(!StringUtils.isEmpty(eforms.PERSON.driv_lic_number))
		{
			if ("NA".equalsIgnoreCase(eforms.PERSON.driv_lic_number) || "NONE".equalsIgnoreCase(eforms.PERSON.driv_lic_number))
			{
				if(eforms.PERSON.driv_lic_state==null) 
				{
					eforms.PERSON.driv_lic_state="N_A";
				}	
			}
		}											
		eforms.PERSON.occupation = eforms.HFA_PAPER_APP.occupation_1;
		eforms.PERSON.employer_name = eforms.HFA_PAPER_APP.employer_name;

		eforms.PERSON.curr_employer_dur =  eforms.HFA_PAPER_APP.curr_employer_dur

		eforms.PERSON.curr_employer_dur_mo = eforms.HFA_PAPER_APP.curr_employer_dur_mo

		eforms.PERSON.annual_earned_income = getNumericValues(eforms.HFA_PAPER_APP.annual_earned_income);
		
		eforms.PERSON.type_of_curr_employ = eforms.HFA_PAPER_APP.type_of_curr_employ;
		
		eforms.PERSON.height_feet = getNumericValues(eforms.HFA_PAPER_APP.height_feet);
		
		eforms.PERSON.height_inches = getNumericValues(eforms.HFA_PAPER_APP.height_inches);
		
		eforms.PERSON.weight_pounds = getNumericValues(eforms.HFA_PAPER_APP.weight_pounds);
		
		eforms.PERSON.citizenship_status = (eforms.HFA_PAPER_APP.CYes == null) ? "N" : "Y";
		eforms.PERSON.oth_owner_ind = (eforms.HFA_PAPER_APP.OOYes == null) ? "N" : "Y";
		
		//SECONDARY ADDRESSEE
		eforms.PERSON.secondary_ind_fl = (eforms.HFA_PAPER_APP.secondary_ind_flYES == null) ? "N" : "Y";
		eforms.PERSON.secondary_ind_seniors_fl = (eforms.HFA_PAPER_APP.secondary_ind_seniors_flYES == null) ? "N" : "Y";
		eforms.PERSON.secondary_ind_seniors = (eforms.HFA_PAPER_APP.secondary_ind_seniorsYES == null) ? "N" : "Y";
		eforms.PERSON.secondary_first_name = eforms.HFA_PAPER_APP.secondary_first_name?:null;
		eforms.PERSON.secondary_middle_name = eforms.HFA_PAPER_APP.secondary_middle_name?:null
		eforms.PERSON.secondary_last_name = eforms.HFA_PAPER_APP.secondary_last_name?:null;
		eforms.PERSON.secondary_address_1 = eforms.HFA_PAPER_APP.secondary_address_1?:null;
		eforms.PERSON.secondary_address_2 = eforms.HFA_PAPER_APP.secondary_address_2?:null;
		eforms.PERSON.secondary_city = eforms.HFA_PAPER_APP.secondary_city?:null;
		eforms.PERSON.secondary_state = eforms.HFA_PAPER_APP.secondary_state?:null;
		eforms.PERSON.secondary_zip = eforms.HFA_PAPER_APP.secondary_zip?:null;
		eforms.PERSON.fl_secondary_first_name = eforms.HFA_PAPER_APP.fl_secondary_first_name?:null;
		eforms.PERSON.fl_secondary_middle_name = eforms.HFA_PAPER_APP.fl_secondary_middle_name?:null;
		eforms.PERSON.fl_secondary_last_name = eforms.HFA_PAPER_APP.fl_secondary_last_name?:null;
		eforms.PERSON.fl_secondary_address_1 = eforms.HFA_PAPER_APP.fl_secondary_address_1?:null;
		eforms.PERSON.fl_secondary_address_2 = eforms.HFA_PAPER_APP.fl_secondary_address_2?:null;	
		eforms.PERSON.fl_secondary_city = eforms.HFA_PAPER_APP.fl_secondary_city?:null;	
		eforms.PERSON.fl_secondary_state = eforms.HFA_PAPER_APP.fl_secondary_state?:null;	
		eforms.PERSON.fl_secondary_zip = eforms.HFA_PAPER_APP.fl_secondary_zip?: null;

		if("Y".equals(eforms.PERSON.oth_owner_ind))
		{
			createPolicyOwner();
		}
		
		//Parse Coverages
		
		
		//Parse AUTHORIZATION
		if (eforms.containsKey("AUTHORIZATION"))
		{
			eforms.AUTHORIZATION.hipaa_date_insured = eforms.HFA_PAPER_APP.hipaa_date_insured;
			eforms.AUTHORIZATION.contract_city = eforms.HFA_PAPER_APP.contract_city;
			eforms.AUTHORIZATION.contract_state = (eforms.HFA_PAPER_APP.contract_state==null) ? 
														null :findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.contract_state).code;
		}												
		// Parse REFLEXIVE											
		eforms.REFLEXIVE.bene_first_name = eforms.HFA_PAPER_APP.bene1_first_name;
		eforms.REFLEXIVE.bene_middle_name = eforms.HFA_PAPER_APP.bene1_middle_name;
		eforms.REFLEXIVE.bene_last_name = eforms.HFA_PAPER_APP.bene1_last_name;
		eforms.REFLEXIVE.bene_relation = (eforms.HFA_PAPER_APP.bene1_relation==null) ? 
													null :findInLookup("RELATIONSHIP_TYPE", eforms.HFA_PAPER_APP.bene1_relation).code;
		eforms.REFLEXIVE["bene_first_name2" + delim + "0"] = eforms.HFA_PAPER_APP.bene2_first_name;
		eforms.REFLEXIVE["bene_middle_name2" + delim + "0"] = eforms.HFA_PAPER_APP.bene2_middle_name;
		eforms.REFLEXIVE["bene_last_name2" + delim + "0"] = eforms.HFA_PAPER_APP.bene2_last_name;
		eforms.REFLEXIVE["bene_relation2" + delim + "0"] = (eforms.HFA_PAPER_APP.bene2_relation==null) ? 
													null :findInLookup("RELATIONSHIP_TYPE", eforms.HFA_PAPER_APP.bene2_relation).code;
		eforms.REFLEXIVE["contingent_bene_first_name2" + delim + "0"] = eforms.HFA_PAPER_APP.contingent_bene1_first_name;
		eforms.REFLEXIVE["contingent_bene_middle_name2" + delim + "0"] = eforms.HFA_PAPER_APP.contingent_bene1_middle_name;
		eforms.REFLEXIVE["contingent_bene_last_name2" + delim + "0"] = eforms.HFA_PAPER_APP.contingent_bene1_last_name;
		eforms.REFLEXIVE["contingent_bene_relation2" + delim + "0"] = (eforms.HFA_PAPER_APP.contingent_bene1_relation==null) ? 
													null :findInLookup("RELATIONSHIP_TYPE", eforms.HFA_PAPER_APP.contingent_bene1_relation).code;
		eforms.REFLEXIVE["contingent_bene_first_name2" + delim + "1"] = eforms.HFA_PAPER_APP.contingent_bene2_first_name;
		eforms.REFLEXIVE["contingent_bene_middle_name2" + delim + "1"] = eforms.HFA_PAPER_APP.contingent_bene2_middle_name;
		eforms.REFLEXIVE["contingent_bene_last_name2" + delim + "1"] = eforms.HFA_PAPER_APP.contingent_bene2_last_name;
		eforms.REFLEXIVE["contingent_bene_relation2" + delim + "1"] = (eforms.HFA_PAPER_APP.contingent_bene2_relation==null) ?
													null :findInLookup("RELATIONSHIP_TYPE", eforms.HFA_PAPER_APP.contingent_bene2_relation).code;
													
		
		eforms.REFLEXIVE.weight_chg_ind = (eforms.HFA_PAPER_APP.WCYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.weight_chg_pounds = getNumericValues(eforms.HFA_PAPER_APP.weight_chg_pounds);
		
		currentCase.getPrimaryApplicant().setTobaccoUsage(Boolean.valueOf(eforms.HFA_PAPER_APP.SYes != null))
		
		eforms.REFLEXIVE.m1a = (eforms.HFA_PAPER_APP.m1aYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1b = (eforms.HFA_PAPER_APP.m1bYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1c = (eforms.HFA_PAPER_APP.m1cYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1d = (eforms.HFA_PAPER_APP.m1dYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1e = (eforms.HFA_PAPER_APP.m1eYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1f = (eforms.HFA_PAPER_APP.m1fYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1fCT = (eforms.HFA_PAPER_APP.m1fYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1g = (eforms.HFA_PAPER_APP.m1gYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1h = (eforms.HFA_PAPER_APP.m1hYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1i = (eforms.HFA_PAPER_APP.m1iYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1j = (eforms.HFA_PAPER_APP.m1jYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m1jCT = (eforms.HFA_PAPER_APP.m1jYes == null) ? "N" : "Y";
		
		eforms.REFLEXIVE.m2 = (eforms.HFA_PAPER_APP.m2Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m3 = (eforms.HFA_PAPER_APP.m3Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m3FL = (eforms.HFA_PAPER_APP.m3Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m3CA = (eforms.HFA_PAPER_APP.m3Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m3CT = (eforms.HFA_PAPER_APP.m3Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m3NY = (eforms.HFA_PAPER_APP.m3Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m4a = (eforms.HFA_PAPER_APP.m4aYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m4b = (eforms.HFA_PAPER_APP.m4bYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m4c = (eforms.HFA_PAPER_APP.m4cYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m4d = (eforms.HFA_PAPER_APP.m4dYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m4e = (eforms.HFA_PAPER_APP.m4eYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m4f = (eforms.HFA_PAPER_APP.m4fYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m4g = (eforms.HFA_PAPER_APP.m4gYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m5a = (eforms.HFA_PAPER_APP.m5aYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m5aCT = (eforms.HFA_PAPER_APP.m5aYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m5b = (eforms.HFA_PAPER_APP.m5bYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m6a = (eforms.HFA_PAPER_APP.m6aYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m6b = (eforms.HFA_PAPER_APP.m6bYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m6bCT = (eforms.HFA_PAPER_APP.m6bYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m7 = (eforms.HFA_PAPER_APP.m7Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m7CA = (eforms.HFA_PAPER_APP.m7Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m8 = (eforms.HFA_PAPER_APP.m8Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m8CT = (eforms.HFA_PAPER_APP.m8Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m8FL = (eforms.HFA_PAPER_APP.m8Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m9a = (eforms.HFA_PAPER_APP.m9aYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m9aFL = (eforms.HFA_PAPER_APP.m9aYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m9aCT = (eforms.HFA_PAPER_APP.m9aYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m9b = (eforms.HFA_PAPER_APP.m9bYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.m9bCT = (eforms.HFA_PAPER_APP.m9bYes == null) ? "N" : "Y";
		
		answerSmokerN = Boolean.valueOf(eforms.HFA_PAPER_APP.SYes != null);
		if (!answerSmokerN)
		{
			eforms.PERSON.smoker_ind="N";
		}
		answerMedicalN = Boolean.valueOf(eforms.HFA_PAPER_APP.WCYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m1aYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m1bYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m1cYes != null) || 
						Boolean.valueOf(eforms.HFA_PAPER_APP.m1dYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m1eYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m1fYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m1gYes != null) || 
						Boolean.valueOf(eforms.HFA_PAPER_APP.m1hYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m1iYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m1jYes != null);
		
		answerMedicalN = answerMedicalN || Boolean.valueOf(eforms.HFA_PAPER_APP.m2Yes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m3Yes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m4aYes != null) || 
						Boolean.valueOf(eforms.HFA_PAPER_APP.m4bYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m4cYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m4dYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m4eYes != null) || 
						Boolean.valueOf(eforms.HFA_PAPER_APP.m4fYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m4gYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m5aYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m5bYes != null) || 
						Boolean.valueOf(eforms.HFA_PAPER_APP.m6aYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m6bYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m7Yes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m8Yes != null) || 
						Boolean.valueOf(eforms.HFA_PAPER_APP.m9aYes != null) || Boolean.valueOf(eforms.HFA_PAPER_APP.m9bYes != null);		

		additionalMedicalNeeded = additionalMedicalNeeded || (DateUtil.calculateAge(DateUtil.convertDate(eforms.HFA_PAPER_APP.date_of_birth)) >=51);

		eforms.REFLEXIVE.additional_info = eforms.HFA_PAPER_APP.additional_info;
		eforms.REFLEXIVE.app_pending_answ = (eforms.HFA_PAPER_APP.IFYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.app_pending_answ_AZ = (eforms.HFA_PAPER_APP.IFYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.app_replace_answ = (eforms.HFA_PAPER_APP.CTLRYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.app_replace_question = (eforms.HFA_PAPER_APP.CTLRYes == null) ? "N" : "Y";
		eforms.REFLEXIVE.oth_ins_company = eforms.HFA_PAPER_APP.oth_ins1_company;
		eforms.REFLEXIVE.oth_ins_city = eforms.HFA_PAPER_APP.oth_ins1_city;
		eforms.REFLEXIVE.oth_ins_state = (eforms.HFA_PAPER_APP.oth_ins1_state==null) ? 
										null : findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.oth_ins1_state).code;
										
		eforms.REFLEXIVE.oth_ins_face = eforms.HFA_PAPER_APP.oth_ins1_face!=null?getNumericValues(eforms.HFA_PAPER_APP.oth_ins1_face):"";	
		eforms.REFLEXIVE.oth_ins_mm = (eforms.HFA_PAPER_APP.oth_ins1_mm==null) ? "": (eforms.HFA_PAPER_APP.oth_ins1_mm as int);
		eforms.REFLEXIVE.oth_ins_yy = (eforms.HFA_PAPER_APP.oth_ins1_yy==null) ? "" : (eforms.HFA_PAPER_APP.oth_ins1_yy as int);
		eforms.REFLEXIVE.oth_ins_replace = (eforms.HFA_PAPER_APP.CREP1Yes == null) ? "N" : "Y";
		eforms.REFLEXIVE["oth_ins_company2" + delim + "0"] = eforms.HFA_PAPER_APP.oth_ins2_company;
		eforms.REFLEXIVE["oth_ins_city2" + delim + "0"] = eforms.HFA_PAPER_APP.oth_ins2_city;
		eforms.REFLEXIVE["oth_ins_state2" + delim + "0"] = (eforms.HFA_PAPER_APP.oth_ins2_state==null) ?
										null : findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.oth_ins2_state).code;
		eforms.REFLEXIVE["oth_ins_face2" + delim + "0"] = eforms.HFA_PAPER_APP.oth_ins2_face!=null?getNumericValues(eforms.HFA_PAPER_APP.oth_ins2_face):"";
		eforms.REFLEXIVE["oth_ins_mm2" + delim + "0"] = (eforms.HFA_PAPER_APP.oth_ins2_mm==null) ? "" : (eforms.HFA_PAPER_APP.oth_ins2_mm as int);
		eforms.REFLEXIVE["oth_ins_yy2" + delim + "0"] = (eforms.HFA_PAPER_APP.oth_ins2_yy==null) ? "" : (eforms.HFA_PAPER_APP.oth_ins2_yy as int);
		eforms.REFLEXIVE["oth_ins_replace2" + delim + "0"] = (eforms.HFA_PAPER_APP.CREP2Yes == null) ? "N" : "Y";

		//Parse APPLICANT_AUTHORIZATION
		if (eforms.containsKey("APPLICANT_AUTHORIZATION"))
		{
			eforms.APPLICANT_AUTHORIZATION.ack_date_insured = eforms.HFA_PAPER_APP.ack_date_insured;
			eforms.APPLICANT_AUTHORIZATION.ack_city = eforms.HFA_PAPER_APP.ack_city;
			eforms.APPLICANT_AUTHORIZATION.ack_state = eforms.HFA_PAPER_APP.ack_state;
			eforms.APPLICANT_AUTHORIZATION.ack_sig_insured = (eforms.HFA_PAPER_APP.ack_sig_insured==null)?"N" : "Y";
			eforms.APPLICANT_AUTHORIZATION.ack_sig_owner = (eforms.HFA_PAPER_APP.ack_sig_owner==null)?"N" : "Y";
			eforms.APPLICANT_AUTHORIZATION.ack_date_owner = eforms.HFA_PAPER_APP.ack_date_owner;
		}
		//Parse AGENT
		if (eforms.containsKey("AGENT_ATTESTATION"))
		{
			eforms.AGENT_ATTESTATION.oth_pending_ind = (eforms.HFA_PAPER_APP.OPYes == null) ? "N" : "Y";
			eforms.AGENT_ATTESTATION.replacement_ind = (eforms.HFA_PAPER_APP.REYes == null) ? "N" : "Y";
			eforms.AGENT_ATTESTATION.y_1035_exch_ind = (eforms.HFA_PAPER_APP.EXCHYes == null) ? "N" : "Y";
			eforms.AGENT_ATTESTATION.internal_term_conv_ind = (eforms.HFA_PAPER_APP.CONVYes == null) ? "N" : "Y";
			eforms.AGENT_ATTESTATION.attest_ind = (eforms.HFA_PAPER_APP.ATTYes == null) ? "N" : "Y";
			//eforms.AGENT_ATTESTATION.agent_date = eforms.HFA_PAPER_APP.agent_date;
			eforms.AGENT_ATTESTATION.agent_id = eforms.HFA_PAPER_APP.agent_id;
			eforms.AGENT_ATTESTATION.agent_license_no = eforms.HFA_PAPER_APP.agent_license_no; //agent_id for Florida
			eforms.AGENT_ATTESTATION.printed_name = eforms.HFA_PAPER_APP.printed_name;
			//eforms.AGENT_ATTESTATION.agent_sig = (eforms.HFA_PAPER_APP.agent_sig == null) ? "N" : "Y";
		}	
		// Agent initialization into case
		def Agent agent;
		def SecurityUser agentSecurityUser;
		if (currentCase.caseSource.equals(CASE_SOURCE.EPAPER))
		{
			def agentCode = eforms.HFA_PAPER_APP.agent_id?: eforms.HFA_PAPER_APP.agent_license_no;
			agentSecurityUser = services.hibernateTemplate.get(SecurityUser.class, "select su from " + SecurityUser.class.getCanonicalName() + " as su where su.username = ?", agentCode);
			agent = services.hibernateTemplate.get(Agent.class, agentSecurityUser.agentId);
			currentCase.setAgentId((agent == null)? null:agent.id);
			currentCase.setAgent((agent == null)? null:agent);
			currentCase.setContractLocale((eforms.HFA_PAPER_APP.agent_license_state==null) ? 
													null :findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.agent_license_state).code);
		}
		else if	(currentCase.caseSource.equals(CASE_SOURCE.IPAPER))
		{
			agent = services.hibernateTemplate.get(Agent.class, currentCase.agentId);
		}	
		def List<CarrierProducerAppointment> carrierProducerAppointment = services.hibernateTemplate.getAll(CarrierProducerAppointment.class, 
				"select cpa from " + CarrierProducerAppointment.class.getCanonicalName() + " as cpa where cpa.agent.id = ?", agent.id);
		currentCase.setLineOfBusinessId(getLobIdForAgent(carrierProducerAppointment));
		
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(SecurityUser.class);
		detachedCriteria.add(Restrictions.eq("agentId", agent.id));
		detachedCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		detachedCriteria.setFetchMode("securityOus", FetchMode.JOIN);
		def List<SecurityUser> securityUserList = services.hibernateTemplate.getAll(detachedCriteria);
		def SecurityUser securityUser = securityUserList.get(0);
		currentCase.setOwner(securityUser.getSecurityOus().get(0), securityUser, true);

		resolveRequirements(answerSmokerN, answerMedicalN);
		
		//resolveSignatureRequirements();
		
		LOGGER.debug("Completed EForm conversion");
	}
	
	private String getLobIdForAgent(List<CarrierProducerAppointment> carrierProducerAppointment)
	{
		String lobId = null;
		if (carrierProducerAppointment != null)
		{
			for (CarrierProducerAppointment apt : carrierProducerAppointment)
			{
				if (apt.getProductId().equals(currentCase.getProductId()))
					lobId = apt.getLobId();
			}
		}
		return lobId;
	}
	
	private void resolveRequirements(boolean allSmokerQuestionAnsweredN, boolean allMedicalQuestionAnsweredN)
	{
		for (CaseRequirement caseRequirement : currentCase.getRequirements())
		{
			if (caseRequirement.getRequirementCode().equalsIgnoreCase("PT3") && (!allMedicalQuestionAnsweredN))
			{
				caseRequirement.setStatus(CaseRequirement.STATUS.RESOLVED);
			}
			else if (caseRequirement.getRequirementCode().equalsIgnoreCase("PT1") && (!allSmokerQuestionAnsweredN))
			{
				caseRequirement.setStatus(CaseRequirement.STATUS.RESOLVED);
			}
		}
		services.hibernateTemplate.saveOrUpdate(currentCase);
	}
	
	private String getNumericValues(String input){
		StringBuffer sb = new StringBuffer();
		
		if(input!=null && input.trim().length()>0){
			Pattern p = Pattern.compile("\\d+");
			Matcher m = p.matcher(input);
			
			while (m.find()) {
			  sb.append(m.group());
			}
		}
		return sb.toString();
	}
	
	def Lookup findInLookup(String lookupType, String lookupValue)
	{
		LOGGER.debug(lookupValue)
		for(Lookup it: lookups)
		{ 
			if(it.name.equalsIgnoreCase(lookupType) && (it.code.equalsIgnoreCase(lookupValue) || it.description.equalsIgnoreCase(lookupValue))) return it
		}
		return new Lookup(code:"", name:"", value:"")
		
	}
	
	public void setupRepeatedSections()
	{
		String delim = EFormBeanXMLAdapter.repeatSectionDelim;
		if ("REFLEXIVE".equalsIgnoreCase(currentEFormCode))
		{
			if (eforms.REFLEXIVE["bene_first_name2" + delim + "0"]!=null && eforms.REFLEXIVE["bene_last_name2" + delim + "0"]!=null)
			{
				System.out.println("Setup primary bene2");
				List<List<Group>> primBeneficiary = new ArrayList<List<Group>>();
				Group groupName = new Group();
				Group groupRelation = new Group();
				groupName.setSectionRepeat(0);
				Question firstName = new Question();
				firstName.setId("bene_first_name2" + delim + "0");
				Question middleName = new Question();
				middleName.setId("bene_middle_name2" + delim + "0");
				Question lastName = new Question();
				lastName.setId("bene_last_name2" + delim + "0");
				List<Question> questions = new ArrayList<Question>();
				questions.add(firstName);
				questions.add(middleName);
				questions.add(lastName);
				groupName.setQuestions(questions);
				Question relation = new Question();
				relation.setId("bene_relation2" + delim + "0");
				questions  = new ArrayList<Question>();
				questions.add(relation);
				groupRelation.setQuestions(questions);
				List<Group> primBene1 = new ArrayList<Group>();
				primBene1.add(groupName);
				primBene1.add(groupRelation);
				primBeneficiary.add(primBene1);
				setSection("Part 3A - Beneficiary Election", "Primary Beneficiary", primBeneficiary)
			}
			if (eforms.REFLEXIVE["contingent_bene_first_name2" + delim + "0"]!=null && eforms.REFLEXIVE["contingent_bene_last_name2" + delim + "0"]!=null)
			{
				List<List<Group>> contBeneficiary = new ArrayList<List<Group>>();
				Group groupName = new Group();
				Group groupRelation = new Group();
				groupName.setSectionRepeat(0);
				Question firstName = new Question();
				firstName.setId("contingent_bene_first_name2" + delim + "0");
				Question middleName = new Question();
				middleName.setId("contingent_bene_middle_name2" + delim + "0");
				Question lastName = new Question();
				lastName.setId("contingent_bene_last_name2" + delim + "0");
				List<Question> questions = new ArrayList<Question>();
				questions.add(firstName);
				questions.add(middleName);
				questions.add(lastName);
				groupName.setQuestions(questions);
				Question relation = new Question();
				relation.setId("contingent_bene_relation2" + delim + "0");
				questions  = new ArrayList<Question>();
				questions.add(relation);
				groupRelation.setQuestions(questions);
				List<Group> contBene1 = new ArrayList<Group>();
				contBene1.add(groupName);
				contBene1.add(groupRelation);
				contBeneficiary.add(contBene1);
				
				if (eforms.REFLEXIVE["contingent_bene_first_name2" + delim + "1"]!=null && eforms.REFLEXIVE["contingent_bene_last_name2" + delim + "1"]!=null)
				{
					Group groupName2 = new Group();
					Group groupRelation2 = new Group();
					groupName2.setSectionRepeat(0);
					Question firstName2 = new Question();
					firstName2.setId("contingent_bene_first_name2" + delim + "1");
					Question middleName2 = new Question();
					middleName2.setId("contingent_bene_middle_name2" + delim + "1");
					Question lastName2 = new Question();
					lastName2.setId("contingent_bene_last_name2" + delim + "1");
					List<Question> questions2 = new ArrayList<Question>();
					questions2.add(firstName2);
					questions2.add(middleName2);
					questions2.add(lastName2);
					groupName2.setQuestions(questions2);
					Question relation2 = new Question();
					relation2.setId("contingent_bene_relation2" + delim + "1");
					questions2  = new ArrayList<Question>();
					questions2.add(relation2);
					groupRelation2.setQuestions(questions2);
					List<Group> contBene2 = new ArrayList<Group>();
					contBene2.add(groupName2);
					contBene2.add(groupRelation2);
					contBeneficiary.add(contBene2);
				}
				setSection("Part 3A - Beneficiary Election", "Contingent Beneficiary", contBeneficiary)
			}	
			if (eforms.REFLEXIVE["oth_ins_company2" + delim + "0"]!=null)
			{
				List<List<Group>> otherCompany = new ArrayList<List<Group>>();
				Group groupName = new Group();
				Group groupAddress = new Group();
				Group groupStreet = new Group();
				Group groupZip = new Group();
				Group groupFace = new Group();
				Group groupNaic = new Group();
				Group groupMmYy = new Group();
				Group groupReplace = new Group();
				groupName.setSectionRepeat(0);
				groupAddress.setSectionRepeat(0);
				groupReplace.setSectionRepeat(0);
				List<Question> questions = new ArrayList<Question>();
				Question companyName = new Question();
				companyName.setId("oth_ins_company2" + delim + "0");
				questions.add(companyName);
				groupName.setQuestions(questions);
				Question city = new Question();
				city.setId("oth_ins_city2" + delim + "0");
				Question state = new Question();
				state.setId("oth_ins_state2" + delim + "0");
				questions = new ArrayList<Question>();
				questions.add(city);
				questions.add(state);
				groupAddress.setQuestions(questions);
				questions = new ArrayList<Question>();
				Question street = new Question();
				street.setId("oth_ins_street2" + delim + "0");
				questions.add(street);
				groupStreet.setQuestions(questions);
				questions = new ArrayList<Question>();
				Question zip = new Question();
				zip.setId("oth_ins_zip2" + delim + "0");
				questions.add(zip);
				groupZip.setQuestions(questions);
				questions = new ArrayList<Question>();
				Question naic = new Question();
				naic.setId("oth_ins_naic2" + delim + "0");
				questions.add(naic);
				groupNaic.setQuestions(questions);
				Question face = new Question();
				face.setId("oth_ins_face2" + delim + "0");
				questions  = new ArrayList<Question>();
				questions.add(face);
				groupFace.setQuestions(questions);
				Question mm = new Question();
				Question yy = new Question();
				mm.setId("oth_ins_mm2" + delim + "0");
				yy.setId("oth_ins_yy2" + delim + "0");
				questions  = new ArrayList<Question>();
				questions.add(mm);
				questions.add(yy);
				groupMmYy.setQuestions(questions);
				Question replace = new Question();
				replace.setId("oth_ins_replace2" + delim + "0");
				questions  = new ArrayList<Question>();
				questions.add(replace);
				groupReplace.setQuestions(questions);			
				List<Group> otherCoverageGroups = new ArrayList<Group>();
				otherCoverageGroups.add(groupName);
				otherCoverageGroups.add(groupAddress);
				otherCoverageGroups.add(groupStreet);
				otherCoverageGroups.add(groupZip);
				otherCoverageGroups.add(groupFace);
				otherCoverageGroups.add(groupMmYy);
				otherCoverageGroups.add(groupNaic);
				otherCoverageGroups.add(groupReplace)
				otherCompany.add(otherCoverageGroups);
				setSection("Part 3E - Coverage Questions", "Other Coverage", otherCompany)
			}
		}	
	}
	
	def void setSection(String pageTitle, String sectionTitle, List<List<Group>> repeatedGroup)
	{
		System.out.println("Setting section: " + pageTitle + " " + sectionTitle);
		for(Page page: currentEForm.getPages())
		{
			if (pageTitle.equalsIgnoreCase(page.getTitle()))
			{
				for (Section section: page.getRepeatingSections())
				{
					if (sectionTitle.equalsIgnoreCase(section.getTitle()))
					{
						section.setRepeatingElements(repeatedGroup);
					}
				}
			}
		}

	}
	
	public void setUpApplicationQuoteItems()
	{
		String delim = EFormBeanXMLAdapter.repeatSectionDelim;
		def String whole_life = (eforms.HFA_PAPER_APP.whole_life == null) ? "N" : "Y";
		def String level_term = (eforms.HFA_PAPER_APP.level_term == null) ? "N" : "Y";
		def String level_period_10 = "N";
		def String level_period_15 = "N";
		def String level_period_20 = "N";
		def String level_period_30 = "N";
		if ("Y".equalsIgnoreCase(level_term))
		{
			level_period_10 = (eforms.HFA_PAPER_APP.years_10 == null) ? "N" : "Y";
			level_period_15 = (eforms.HFA_PAPER_APP.years_15 == null) ? "N" : "Y";
			level_period_20 = (eforms.HFA_PAPER_APP.years_20 == null) ? "N" : "Y";
			level_period_30 = (eforms.HFA_PAPER_APP.years_30 == null) ? "N" : "Y";	
		}
		def String face_amount = getNumericValues(eforms.HFA_PAPER_APP.requested_face);
		def BigDecimal coverageAmount = new BigDecimal(face_amount) * 1000;
		def String gir = (eforms.HFA_PAPER_APP.GIRYes == null) ? "N" : "Y";
		def String wop = (eforms.HFA_PAPER_APP.WOPYes == null) ? "N" : "Y";
		
		for (CoveragePlan coveragePlan: coveragePlanList)
		{
			for (CoverageOption option : coveragePlan.getCoverageOptions())
			{
				if ("FA".equalsIgnoreCase(option.getOptionCode()))
				{
					option.setSelected(true);
					option.setSelectedValue(face_amount);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSkipProcessCoverageAmount(true);
				}
				if (("TERM30".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(level_period_30)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
				if (("TERM20".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(level_period_20)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
				if (("TERM15".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(level_period_15)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
				if (("TERM10".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(level_period_10)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
				if (("WL".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(whole_life)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
				if (("GIR".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(gir)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
				if (("TERMDWP".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(wop)) && ("N".equalsIgnoreCase(level_period_30)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
				if (("TERM30DWP".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(wop)) && ("Y".equalsIgnoreCase(level_period_30)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
				if (("WLDWP".equalsIgnoreCase(option.getOptionCode())) && ("Y".equalsIgnoreCase(wop)) && ("Y".equalsIgnoreCase(whole_life)))
				{
					option.setSelected(true);
					option.setSelectedValueCoverageAmount(coverageAmount);
					option.setSelectedValue(coverageAmount.toString());
				}
			}
		}
	}
	
	
	def void createPolicyOwner()
	{
		Applicant applicant = new Applicant();
		applicant.setApplicantType(APPLICANT_TYPE.POLICY_OWNER);
		applicant.setUwType(APPLICANT_UW_TYPE.NOT_APPLICABLE);
		applicant.setApplicationCase(currentCase);
		currentCase.getApplicants().add(applicant);
	}
	
	
	def void parsePrimaryApplicant()
	{
		Applicant applicant = new Applicant();
		applicant.setApplicantType(APPLICANT_TYPE.PRIMARY_INSURED);
		applicant.setIsPrimary(true);
		applicant.setUwType(APPLICANT_UW_TYPE.PRIMARY_UNDERWRITTEN);
		applicant.setApplicationCase(currentCase);
		Address address = new Address();
		address.setType(1);
		applicant.setApplicationCase(currentCase);
		applicant.setFirstName(eforms.HFA_PAPER_APP.first_name);
		applicant.setLastName(eforms.HFA_PAPER_APP.last_name);
		applicant.setMiddleName(StringUtils.isEmpty(eforms.HFA_PAPER_APP.middle_initial)?'':eforms.HFA_PAPER_APP.middle_initial);
		applicant.setGender((eforms.HFA_PAPER_APP.Male != null) ? 1 : 0);
		applicant.setBirthdate(DateUtil.convertDate(eforms.HFA_PAPER_APP.date_of_birth));
		applicant.setGovernmentId(eforms.HFA_PAPER_APP.social_security_number);
		applicant.setEmail(eforms.HFA_PAPER_APP.email_address);
		applicant.setPhone(eforms.HFA_PAPER_APP.primaryPhone);
		def phoneType = [eforms.HFA_PAPER_APP.phone1_business, eforms.HFA_PAPER_APP.phone1_home, eforms.HFA_PAPER_APP.phone1_mobile].find { it }
		applicant.setPhoneType(ReflectionUtils.enumValue(PHONE_TYPE.class, phoneType.find { it ==~ 'BUSINESS|MOBILE|HOME' }))
		applicant.setSecondaryPhone(eforms.HFA_PAPER_APP.secondaryPhone);
		phoneType = [eforms.HFA_PAPER_APP.phone2_business, eforms.HFA_PAPER_APP.phone2_home, eforms.HFA_PAPER_APP.phone2_mobile].find { it }
		applicant.setSecondaryPhoneType(ReflectionUtils.enumValue(PHONE_TYPE.class, phoneType.find { it ==~ 'BUSINESS|MOBILE|HOME' }));
		address.setAddressLine1(eforms.HFA_PAPER_APP.legal_residence_address);
		address.setCity(eforms.HFA_PAPER_APP.legal_city);
		address.setState(findInLookup("LOCALE_STATE", eforms.HFA_PAPER_APP.legal_residence_state).code);
		address.setZip(eforms.HFA_PAPER_APP.legal_zip);
		address.setValidated(false);
		List<Applicant> applicants = new ArrayList<Applicant>();
		List<Address> addresses = new ArrayList<Address>();
		addresses.add(address);
		applicant.setAddresses(addresses);
		applicant.setPrimaryAddress(address);
		applicants.add(applicant);
		currentCase.setApplicants(applicants);
	}	
}