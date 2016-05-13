package HFA.scripts;

import org.apache.commons.lang.StringUtils

import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.policy.*
import com.stepsoln.core.db.policy.AbstractDepBen.BENEFICIARY_TYPE
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*

def eform
Case currentCase
Services services

def formLoad() {
	loadBeneficiaries();
}

def formNew() {
	System.out.println("new form");
}

def pageOneEnter() {
	System.out.println("page one enter");
}

def importData() {
	importBeneficiaries();
	if (eform['app_replace_question'] && eform['app_replace_question'] == 'Y')
	{
		eform['app_replace_answ'] = 'Y'
	}
	else
	{
		eform['app_replace_answ'] = 'N'
	}
	eformBean.update();
	importCaseExistingInsurances();
}

def importCaseExistingInsurances() 
{
	logger.debug("Start importing CaseExistingInsurances");
	logger.debug("Start importing CaseExistingInsurances:  " + keys);
	try {
	keys = eform.keySet();
	def caseExistingInsurances = [];
	def existingrecords = [];

	for(Object k: keys) 
	{
		String key = (String)k;

		if (key.startsWith("oth_ins_company")) 
		{
			
			logger.debug("Processing data for key: "+key);
			
			if (existingrecords.contains(key))
			{
				continue;
			}

			existingrecords.add(key);

			def oth_ins_company = eform.get(key);

			def oth_ins_city;
			def oth_ins_face;
			def oth_ins_state;
			def oth_ins_street;
			def oth_ins_zip;
			def oth_ins_mmyy
			def oth_ins_naic
			def oth_ins_replace
			def String[] values;
			if (key.equalsIgnoreCase("oth_ins_company")) 
			{
				
				oth_ins_street = eform.get("oth_ins_street");
				oth_ins_zip = eform.get("oth_ins_zip");
				oth_ins_city = eform.get("oth_ins_city");
				oth_ins_state = eform.get("oth_ins_state");
				oth_ins_naic = eform.get("oth_ins_naic");
				oth_ins_face =  eform.get("oth_ins_face");
				oth_ins_mmyy = eform.get("oth_ins_mm")+"/"+eform.get("oth_ins_yy");
				oth_ins_replace = eform.get("oth_ins_replace");
				logger.debug(oth_ins_company + "\n" + oth_ins_city+ "\n" + oth_ins_state + "\n" + oth_ins_zip + "\n" + oth_ins_naic);
				
			}
			else
			{
				def keySuffix = key.substring(16);
				oth_ins_street = eform.get("oth_ins_street2"+ keySuffix);
				oth_ins_zip = eform.get("oth_ins_zip2"+ keySuffix);
				oth_ins_city = eform.get("oth_ins_city2"+ keySuffix);
				oth_ins_state = eform.get("oth_ins_state2"+ keySuffix);
				oth_ins_naic = eform.get("oth_ins_naic2"+ keySuffix);
				oth_ins_face = eform.get("oth_ins_face2" + keySuffix);
				oth_ins_mmyy = eform.get("oth_ins_mm2"+keySuffix)+"/"+eform.get("oth_ins_yy2"+keySuffix);
				oth_ins_replace = eform.get("oth_ins_replace2" + keySuffix);
			}

			def CaseExistingInsurance caseExistingInsurance = new  CaseExistingInsurance();
			int naicPos = StringUtils.indexOf(oth_ins_company, ":");
			if (naicPos != -1)
			{
				oth_ins_company = StringUtils.substring(oth_ins_company, 0, naicPos);
			}
			if (StringUtils.isNotBlank(oth_ins_company) && StringUtils.isNotBlank(oth_ins_replace) 
				&& StringUtils.isNotBlank(oth_ins_mmyy) && oth_ins_face!=null && StringUtils.isNotBlank(oth_ins_face.toString()))
			{
				caseExistingInsurance.setNaicCompanyName(oth_ins_company);
				caseExistingInsurance.setNaicCompanyNumber(oth_ins_naic);
				caseExistingInsurance.setMailingCity(oth_ins_city);
				caseExistingInsurance.setMailingState(oth_ins_state);
				caseExistingInsurance.setMailingStreet(oth_ins_street);
				caseExistingInsurance.setMailingZipCode(oth_ins_zip);
				caseExistingInsurance.setIssueMonthYear(oth_ins_mmyy);
				oth_ins_face = (oth_ins_face==null)?"0":oth_ins_face;
				caseExistingInsurance.setCoverageAmount(new BigDecimal(oth_ins_face));
				
	
				if(StringUtils.isNotBlank(oth_ins_replace)) 
				{
					if(oth_ins_replace.equals("Y")) 
					{
						caseExistingInsurance.setIsReplacement(1);
					}
					else if(oth_ins_replace.equals("N")) 
					{
						caseExistingInsurance.setIsReplacement(0);
					}
				}
	
				caseExistingInsurances.add(caseExistingInsurance);
			}
		}
	}
	
	if (caseExistingInsurances.size() > 0) 
	{
		saveCaseExistingInsurances(caseExistingInsurances);
	}
	
	}catch (Exception e) {
		logger.error("Failed to execute importCaseExistingInsurances script!",e);
	}
}

def importBeneficiaries() 
{
	logger.debug("Start importing beneficiaries");
	try {
		def eFormBeneficiaries = [];
		eFormBeneficiaries = findBeneficiary();
		mergeAndSaveCaseBeneficiaries(eFormBeneficiaries);
	}
	catch (Exception e) {
		logger.error("Failed to execute importBeneficiaries script!",e);
	}
	logger.debug("End importing beneficiaries");
}

def findBeneficiary() 
{
	keys = eform.keySet();
	def bens=[];

	for(Object k: keys) {
		String key = (String)k;
		String othKey = key;
		if (key.startsWith("bene_first_name") || key.startsWith("contingent_bene_first_name")) {
			def benFirstName=eform.get(key);
			othKey = key.replaceFirst("first", "last");
			def benLastName=eform.get(othKey);
			othKey = key.replaceFirst("first", "middle");
			def benMiddleName=eform.get(othKey);
			def relation ="";
			if (key.startsWith("bene_first_name")) {
				if (key.equalsIgnoreCase("bene_first_name")) {
					othKey="bene_relation";
				}
				else {
					othKey="bene_relation2" + key.substring(16);
				}
			}
			else {
				if (key.equalsIgnoreCase("contingent_bene_first_name")) {
					othKey="contingent_bene_relation";
				}
				else {
					othKey="contingent_bene_relation2" + key.substring(27);
				}
			}
			relation = eform.get(othKey);

			logger.debug('Search for beneficiary data: ' + key + ', ' + othKey);



			if( benLastName && benLastName.trim() ) {
				def Beneficiary ben = new Beneficiary(); //create a beneficiary instance when at least last name can be parsed
				if (key.startsWith("bene"))
				{
					ben.setBeneficiaryType(AbstractDepBen.BENEFICIARY_TYPE.PRIMARY);
				}
				else
				{
					ben.setBeneficiaryType(AbstractDepBen.BENEFICIARY_TYPE.CONTINGENT);
				}
				ben.setLastName(benLastName.trim())

				if( benFirstName && benFirstName.trim() )
				{
					ben.setFirstName(benFirstName.trim());
				}
				if( benMiddleName && benMiddleName.trim() )
				{
					ben.setMiddleName(benMiddleName.trim());
				}

				//try to parse relationship when beneficiary name can be parsed
				if( relation && relation.trim() )
				{
					relationshipType=relation.trim().toUpperCase();
					ben.setRelationship(relationshipType);
				}
				bens.add(ben);
			}

		}
	}
	bens //return value
}

def saveCaseExistingInsurances(caseExistingInsurances)
{
	logger.debug("Saving " + caseExistingInsurances.size() + " new caseExistingInsurances");
	
	oldCaseExistingInsurances = services.hibernateTemplate.getAll(CaseExistingInsurance.class, "select b from " + CaseExistingInsurance.class.getCanonicalName() + 
		" as b where b.case = ?", currentCase);
	
	logger.debug("Removing " + oldCaseExistingInsurances.size() + " old caseExistingInsurances");
	oldCaseExistingInsurances.each { services.hibernateTemplate.delete(it);};

	for (CaseExistingInsurance caseExistingInsurance: caseExistingInsurances)
	{
		if (null != caseExistingInsurance)
		{
			caseExistingInsurance.setCase(currentCase);
			services.hibernateTemplate.save(caseExistingInsurance);

			logger.debug("caseExistingInsurance " + caseExistingInsurance.getCaseExistingInsuranceId() + " saved.");
		}
	}
	currentCase.setExistingInsurance(caseExistingInsurances);
}

def mergeAndSaveCaseBeneficiaries(eFormBeneficiaries)
{
	logger.debug("Saving " + eFormBeneficiaries.size() + " new bens");
	oldBens = services.hibernateTemplate.getAll(Beneficiary.class, "select b from "+Beneficiary.class.getCanonicalName()+" as b where b.applicationCase=?",currentCase);
	logger.debug("Removing " + oldBens.size() + " old bens");
	oldBens.each { services.hibernateTemplate.delete(it);};
	bens = new ArrayList<Beneficiary>();
	currentCase.setBeneficiaries(bens);
	//services.hibernateTemplate.update(currentCase);//to clear possibly existing beneficiaries
	for(Beneficiary ben: eFormBeneficiaries)
	{
		if (null!=ben)
		{
			ben.setApplicationCase(currentCase);
			services.hibernateTemplate.save(ben);
			bens.add(ben);
			logger.debug("Ben " + ben.getId() + " saved.");
		}
	}
	currentCase.setBeneficiaries(bens);
}

def loadBeneficiaries() 
{
	for (Beneficiary ben: currentCase.getBeneficiaries()) {
		int primary_index=1;
		int contingent_index=1;
		if (ben.getBeneficiaryType() == BENEFICIARY_TYPE.PRIMARY) 
		{
			String index= (primary_index==1)?"":primary_index
			eform.put("bene_first_name"+index, ben.getFirstName());
			eform.put("bene_middle_name"+index, ben.getMiddleName());
			eform.put("bene_last_name"+index, ben.getLastName());
			eform.put("bene_relation"+index, ben.getRelationship());
			primary_index++;
		} 
		else 
		{
			eform.put("cpntingent_bene_first_name"+contingent_index, ben.getFirstName());
			eform.put("cpntingent_bene_middle_name"+contingent_index, ben.getMiddleName());
			eform.put("cpntingent_bene_last_name"+contingent_index, ben.getLastName());
			eform.put("cpntingent_bene_relation"+contingent_index, ben.getRelationship());
			contingent_index++;
		}
	}
}
