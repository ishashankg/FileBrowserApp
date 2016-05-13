package forms.FSM;

import org.slf4j.Logger;
import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.policy.*
import com.stepsoln.core.eform.EForm;
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*
import com.stepsoln.eapp.db.enums.*
import com.stepsoln.core.db.services.util.scripting.Services
import java.util.Date;
import java.text.SimpleDateFormat;
import com.stepsoln.core.rules.coverages.CoveragePlan;
import com.stepsoln.core.rules.coverages.CoverageOption;
import com.stepsoln.core.eform.EFormAnswers;

def Map<String, EFormAnswers> eform;
def Case currentCase;
def List<CoveragePlan> coveragePlanList;
def Services services;
def com.stepsoln.core.db.services.CaseService caseService;
	
	def postCoverage()
	{
		importDependentsBeneficiaries();
		changeCoverage();
	}

  	def importDependentsBeneficiaries()
  	{
		LOGGER.info("Start importing dependents and beneficiaries");
		try
		{
			LOGGER.info("Start importing beneficiaries");
			def eFormBeneficiaries = [];
			eFormBeneficiaries = findBeneficiary();
			mergeAndSaveCaseBeneficiaries(eFormBeneficiaries);
			LOGGER.info("Start importing dependents");
			def eFormDependents = [];
			eFormDependents = findDependents();
			mergeAndSaveCaseDependents(eFormDependents);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			LOGGER.error("Failed to execute importBeneficiaries script!",e);
		}
		LOGGER.info("End importing dependents and beneficiaries");
  	}
	  
	  def changeCoverage()
	  {
		  def String proposedMedical9a1;
		  def String proposedMedical8a1;
		  proposedMedical9a1 = eform.POSTCOVERAGE.proposedMedical9a1;
		  proposedMedical8a1 = eform.POSTCOVERAGE.proposedMedical8a1;
		  System.out.println("proposedMedical9a1 " + proposedMedical9a1);
		  System.out.println("proposedMedical8a1 " + proposedMedical8a1);
		  if (("1".equalsIgnoreCase(proposedMedical9a1)) || ("1".equalsIgnoreCase(proposedMedical8a1)))
		  {
			  for (CoveragePlan coveragePlan: coveragePlanList)
			  {
				  for (CoverageOption option : coveragePlan.getCoverageOptions())
				  {
					  if ("STFAGOV".equalsIgnoreCase(option.getOptionCode()))
					  {
						  option.setSelected(false);
					  }
				  }
			  }
		  }
	  }
	  
  
	def findDependents()
	{
		/*
		    <DepFName_0>dep1</DepFName_0>
			<DepLName_0>patrow</DepLName_0>
			<DepDOB_0>12/21/1980</DepDOB_0>
			<Depsex1_0>F</Depsex1_0>
			<DepRel_0>Spouse</DepRel_0>
			<DepSSN_0>123-45-6789</DepSSN_0>
		 */
		def EFormAnswers eforms = eform.OTHERINFO;
		keys = eforms.keySet();
		def bens=[];
		
		for(Object k: keys)
		{
			String key = (String)k;
			String othKey = key;
			if (key.startsWith("DepFName")) 
			{
				def benFirstName=eforms.get(key);
				othKey = key.replaceFirst("FName", "LName");  //last name
				def benLastName=eforms.get(othKey);
				othKey = key.replaceFirst("FName", "MiddleIni"); //middle name
				def benMiddleName=eforms.get(othKey);
				othKey = key.replaceFirst("FName", "Rel");  //relation
				def benRelation ="";
				benRelation = eforms.get(othKey);
				
				othKey = key.replaceFirst("FName", "DOB"); //date of birth
				def benDOB = eforms.get(othKey);
				othKey = key.replaceFirst("FName", "sex1"); //sex
				def benSex ="";
				benSex = eforms.get(othKey);
				othKey = key.replaceFirst("FName", "SSN"); //SSN
				def benSSN ="";
				benSSN = eforms.get(othKey);
				
				
				//Only save if there is a last name
				if( benLastName && benLastName.trim() ) 
				{
					def Dependant ben = new Dependant(); //create a dependent instance when at least last name can be parsed
									
					ben.setLastName(benLastName.trim())
			
					if( benFirstName && benFirstName.trim() )
					{
						ben.setFirstName(benFirstName.trim());
					}
					if( benMiddleName && benMiddleName.trim() )
					{
						ben.setMiddleName(benMiddleName.trim());
					}
					if ( benDOB )
					{
						try{
							ben.setBirthdate(benDOB);
						}catch( Exception e){
							e.printStackTrace();
							LOGGER.info("Bad date format - " + benDOB);
						}
					}
								
					//try to parse relationship when beneficiary name can be parsed
					if( benRelation && benRelation.trim() )
					{
						if (AbstractDepBen.RELATIONSHIP_TYPE.UNIFORM_GIFTS_TO_MINORS.displayName.equalsIgnoreCase(benRelation.trim()))
						{
							relationshipType = AbstractDepBen.RELATIONSHIP_TYPE.UNIFORM_GIFTS_TO_MINORS;
						}
						else
						{
							relationshipType = AbstractDepBen.RELATIONSHIP_TYPE.valueOf(benRelation.trim().toUpperCase());
						}
						ben.setRelationship(relationshipType.name());
					}
					if( benSex && benSex.trim() )
					{
						ben.setGender(new Integer(benSex.trim()));
					}
					if( benSSN && benSSN.trim() )
					{
						ben.setGovernmentId(benSSN.trim());
					}
					bens.add(ben);
				}
				
			}
		}
		LOGGER.info((bens.size()==0?"Not":"Yes!") + " found dependents.");
		bens //return value
	}
	
	def findBeneficiary()
	{
		def EFormAnswers eforms = eform.OTHERINFO;
		keys = eforms.keySet();
		def bens=[];
		
		for(Object k: keys)
		{
			String key = (String)k;
			String othKey = key;
			if (key.startsWith("BeneFName"))
			{
				def benFirstName=eforms.get(key);
				othKey = key.replaceFirst("FName", "LName");  //last name
				def benLastName=eforms.get(othKey);
				othKey = key.replaceFirst("FName", "MiddleIni"); //middle name
				def benMiddleName=eforms.get(othKey);
				othKey = key.replaceFirst("FName", "Rel");  //relation
				def benRelation ="";
				benRelation = eforms.get(othKey);
				
				othKey = key.replaceFirst("FName", "DOB"); //date of birth
				def benDOB = eforms.get(othKey);
				othKey = key.replaceFirst("FName", "sex1"); //sex
				def benSex ="";
				benSex = eforms.get(othKey);
				othKey = key.replaceFirst("FName", "SSN"); //SSN
				def benSSN ="";
				benSSN = eforms.get(othKey);
				othKey = key.replaceFirst("FName", "Per"); //SSN
				def benPer ="";
				benPer = eforms.get(othKey);
				othKey = key.replaceFirst("FName", "Type"); //SSN
				def benType ="";
				benType = eforms.get(othKey);
				
				
				//Only save if there is a last name
				if( benLastName && benLastName.trim() )
				{
					def Beneficiary ben = new Beneficiary(); //create a beneficiary instance when at least last name can be parsed
					
					ben.setLastName(benLastName.trim());
			
					if( benType && benType.trim() )
					{
						benType = AbstractDepBen.BENEFICIARY_TYPE.valueOf(benType.trim().toUpperCase());
						ben.setBeneficiaryType(benType);
					}else{
						ben.setBeneficiaryType(AbstractDepBen.BENEFICIARY_TYPE.PRIMARY);
					}
				
					
					if( benFirstName && benFirstName.trim() )
					{
						ben.setFirstName(benFirstName.trim());
					}
					if( benMiddleName && benMiddleName.trim() )
					{
						ben.setMiddleName(benMiddleName.trim());
					}
					if ( benDOB )
					{
						try{
							ben.setBirthdate(benDOB);
						}catch( Exception e){
							e.printStackTrace();
							LOGGER.info("Bad date format - " + benDOB);
						}
					}
								
					//try to parse relationship when beneficiary name can be parsed
					if( benRelation && benRelation.trim() )
					{
						System.out.println("Relationship " + benRelation.trim());
						if (AbstractDepBen.RELATIONSHIP_TYPE.UNIFORM_GIFTS_TO_MINORS.displayName.equalsIgnoreCase(benRelation.trim()))
						{
							relationshipType = AbstractDepBen.RELATIONSHIP_TYPE.UNIFORM_GIFTS_TO_MINORS;
						}
						else
						{
							relationshipType = AbstractDepBen.RELATIONSHIP_TYPE.valueOf(benRelation.trim().toUpperCase());
						}
						ben.setRelationship(relationshipType.name());
					}
					if( benSex && benSex.trim() )
					{
						ben.setGender(new Integer(benSex.trim()));
					}
					if( benSSN && benSSN.trim() )
					{
						ben.setGovernmentId(benSSN.trim());
					}
					if( benPer && benPer.trim() )
					{
						ben.setPercentage(new BigDecimal(benPer.trim()));
					}
					bens.add(ben);
				}
				
			}
		}
		LOGGER.info((bens.size()==0?"Not":"Yes!") + " found beneficiaries.");
		bens //return value
	}
  
	def mergeAndSaveCaseBeneficiaries(eFormBeneficiaries)
	{
		caseService.applicationDataService.saveBeneficiaries(eFormBeneficiaries,currentCase);
	}
	
	def mergeAndSaveCaseDependents(eFormDependents)
	{
		caseService.applicationDataService.saveDependents(eFormDependents,currentCase);
	}
