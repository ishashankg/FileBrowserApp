package HFA.scripts;

import java.util.*
import groovy.xml.MarkupBuilder
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import com.itextpdf.text.Chunk
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Element
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.stepsoln.core.db.Lookup
import com.stepsoln.core.db.carrier.CarrierSequence.SEQUENCE_TYPE
import com.stepsoln.core.db.cases.Applicant
import com.stepsoln.core.db.cases.ApplicationQuote
import com.stepsoln.core.db.cases.ApplicationQuoteItem
import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.db.cases.CaseUser
import com.stepsoln.core.db.cases.CaseRequirement
import com.stepsoln.core.db.cases.ApplicationQuote.QUOTE_TYPE
import com.stepsoln.core.db.cases.Case.CASE_SOURCE
import com.stepsoln.core.db.cases.Case.CASE_STATUS
import com.stepsoln.core.db.cases.Case.UW_CASE_STATUS
import com.stepsoln.core.db.cases.CaseRequirement.CASE_REQUIREMENT_TARGET
import com.stepsoln.core.db.enums.REQUIREMENT_TYPE;
import com.stepsoln.core.db.cases.CaseRequirement.SIGNATURE_STATUS
import com.stepsoln.core.db.cases.CaseRequirement.STATUS
import com.stepsoln.core.db.config.ConfigResource
import com.stepsoln.core.db.enums.APPLICANT_TYPE
import com.stepsoln.core.db.enums.PLAN_TYPE
import com.stepsoln.core.db.party.Agent
import com.stepsoln.core.db.product.ProductPlanAvailability
import com.stepsoln.core.db.security.SecurityInvitation;
import com.stepsoln.core.db.services.CoreServices
import com.stepsoln.core.db.services.SequenceService
import com.stepsoln.core.db.services.util.DialogButtonConfig
import com.stepsoln.core.db.services.util.DialogConfig
import com.stepsoln.core.db.services.util.UserInterfaceFacesBehavior
import com.stepsoln.core.eform.EFormAnswers
import com.stepsoln.core.hibernate.StepHibernateTemplate
import com.stepsoln.core.util.DateUtil
import com.stepsoln.core.util.QuickMap
import com.stepsoln.core.util.SystemConfiguration
import com.stepsoln.core.util.pdfreports.AbstractItextReport
import com.stepsoln.core.util.pdfreports.PdfUtil
import com.stepsoln.servicebus.api.EmailRequestType
import com.stepsoln.servicebus.api.XMLParseHelper
import com.stepsoln.core.util.TemplateHelper
import com.stepsoln.core.util.SystemConfiguration

def Case currentCase;
def List<CaseRequirement> allRequirements;
def Logger logger;
def Map<String, Map<String, Object>> eformsAnswers;
def List<Lookup> lookups;
def String uwRiskCalcStatusReason;
def String uwDecisionDate;
def String uwQuickDecisionInd; 
def StepHibernateTemplate hibernateTemplate;
def CoreServices coreServices;

def String getProductVersionCode()
{
	def String productVersionCode = "";
	boolean wholeLife = false;
	String term = "";
	int band = 1;
	cutOffProperty = SystemConfiguration.current().getCarrierProperty("GLIC", "cutOffDate")
	Date cuttOffDate=DateUtil.parseDate(DateUtil.FORMAT_SLASH_DATE, cutOffProperty);

	List<ApplicationQuote>quotes= new ArrayList(currentCase.getApplicationQuotes());
	Applicant insured = currentCase.getPrimaryApplicant();
	if (quotes.size() > 0)
	{
		ApplicationQuote quote = quotes.get(quotes.size() - 1);
		String stepUnderwritingClass = quote.getUnderwritingClass().getType();
		BigDecimal faceAmount = 0;
		for (ApplicationQuoteItem item: quote.getApplicationQuoteItems())
		{
			ProductPlanAvailability cppa = item.getProductPlanAvailability();
			String option = cppa.getPlanOption().getOptionCode();
			def terms = ["TERM10","TERM15","TERM20","TERM30"]

			if (cppa.getPlanType() == PLAN_TYPE.BASE && "FA".equalsIgnoreCase(option))
			{
				faceAmount = item.getCoverageAmount();
				if (faceAmount >= 25000 && faceAmount < 100000)
				{
					band = 1;
				}
				else if (faceAmount >= 100000 && faceAmount < 300000)
				{
					band = 2;
				}
				else if (faceAmount >= 300000 && faceAmount < 500000)
				{
					band = 3;
				}
				else if (faceAmount >= 500000 && faceAmount <= 1000000)
				{
					band = 4;
				}
			}
			else if (cppa.getPlanType()==PLAN_TYPE.BASE)
			{
				if ("WL".equalsIgnoreCase(option))
				{
					wholeLife = true;
				}
				else
				{
					if (terms.contains(option))
					{
						wholeLife = false;
						term = option;
					}
				}
			}
		}
		
		def term10 = ["N", "O", "P", "J"];
		if (wholeLife)
		{
			//calculate 1-st letter
			if ("MT".equalsIgnoreCase(currentCase.getContractLocale()))
			{
				if(currentCase.getCreatedDate().before(cuttOffDate)){
				productVersionCode += "M";
				} else if(currentCase.getCreatedDate().after(cuttOffDate)){
				productVersionCode += "T";
				}
			}
			else
			{
				if(currentCase.getCreatedDate().before(cuttOffDate)){
					productVersionCode += "L";
				} else if(currentCase.getCreatedDate().after(cuttOffDate)){
					productVersionCode += "S";
				}
			}		
			//calculate 2-nd letter
			productVersionCode += "8";
			//calculate 3-rd letter
			productVersionCode += term10[band-1];
		}
		else
		{
			if (StringUtils.isEmpty(term))
			{
				return "";
			}
			//calculate 1-st letter
			if ("MT".equalsIgnoreCase(currentCase.getContractLocale()))
			{
				productVersionCode += "P";
			}
			else
			{
				productVersionCode += "N";
			}
			//calculate 2-nd letter
			productVersionCode += "8";
			//calculate 3-rd letter			
			def term15 = ["B", "C", "D", "K"];
			def term20 = ["Q", "R", "S", "L"];
			def term30 = ["E", "F", "G", "M"];
			def termsMap = ["TERM10":term10, "TERM15":term15, "TERM20":term20, "TERM30":term30]
			productVersionCode += termsMap.get(term)[band-1];
		}
		//calculate 4-th letter
		// Map: 1st letter = band, 2&3 = Underwriting class, 4th = 0 (non-smoking), 1-(smoking)
		def map = ["3PP0":"0","4PP0":"0","1ST0":"1","2ST0":"1","3ST0":"1","4ST0":"1","1ST1":"2","2ST1":"2",
					"3ST1":"2","4ST1":"2","2PF0":"3","3PF0":"3","4PF0":"3","3PF1":"4","4PF1":"4",
					"1TC0":"A","2TC0":"A","3TC0":"A","4TC0":"A","1TF0":"D","2TF0":"D","3TF0":"D","4TF0":"D",
					"1TC1":"E","2TC1":"E","3TC1":"E","4TC1":"E","1TF1":"H","2TF1":"H","3TF1":"H","4TF1":"H"];
		def String code = band + stepUnderwritingClass + (insured.getTobaccoUsage()?"1":"0");
		logger.debug("Code to match: " + code);
		productVersionCode += (map.get(code.trim())==null) ? "" : map.get(code.trim());
		//calculate 5-th letter
		if (insured.getGender()==0)
		{
			productVersionCode += "2";
		}
		else
		{
			productVersionCode += "1";
		}
		if (StringUtils.isEmpty(productVersionCode) || productVersionCode.length()!=5)
		{
			productVersionCode = (wholeLife) ? "DECWL" : "DECTM";
		}
		return productVersionCode;
	}
}

def String getPlanName()
{
	String planName = "hfa_term";
	for (ApplicationQuoteItem item: currentCase.getFinalQuote().getApplicationQuoteItems())
	{
		if ("WL".equalsIgnoreCase(item.getProductPlanAvailability().getPlanOption().getOptionCode()))
		{
			planName = "hfa_whole";
			break;
		}
	}
	return planName;
}

def getMarketingKeyInfo()
{
	String planName = getPlanName()
	def defaultMarketingKeys = [internal_hfa_term : '007854', internal_hfa_whole : '007856',
				ipaper_hfa_term : '402876', ipaper_hfa_whole : '402877',
				external_hfa_term : '406431', external_hfa_whole : '406432']
	Case theCaseData = currentCase
	String source = (theCaseData.getCaseSource() == CASE_SOURCE.IPAPER || theCaseData.getCaseSource() == CASE_SOURCE.CAPTIVE) && theCaseData.convertedPaperCase != null && theCaseData.convertedPaperCase==true ? 'ipaper' :
					theCaseData.getCaseSource() == CASE_SOURCE.CAPTIVE && theCaseData.convertedPaperCase==null ? 'internal' :
					theCaseData.getCaseSource() == CASE_SOURCE.EXTERNAL ? 'external' : null

	[planName : planName, defaultMarketingKey:(defaultMarketingKeys[source + '_' + planName] ?: null)]
}

def List<String> getExtensionInfo()
{
	def result = [];
	String m7 = eformsAnswers.REFLEXIVE.m7;
	String m7completer = eformsAnswers.REFLEXIVE.m7completer;
	writer = new StringWriter()
	
	if (!StringUtils.isEmpty(m7))
	{

		String tcValue = (m7=="Y")?'1':'0';
		String value = (m7=="Y")? 'TRUE':'FALSE';
		xml = new MarkupBuilder(writer)
		xml.FutureTravelInd(tc:tcValue, xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, value);
		result.add(writer.toString());
		writer.getBuffer().setLength(0);
	}
	if (!StringUtils.isEmpty(m7completer) && !StringUtils.isEmpty(m7) && m7=="Y")
	{
		xml1 = new MarkupBuilder(writer);
		xml1.AdditionalInformation(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, m7completer);
		result.add(writer.toString());
	}
	return result;
}

def List<String> getTreatmentDescription()
{
	def result = [];
	String m4c = eformsAnswers.REFLEXIVE.m4c;
	String m4ccompleter = eformsAnswers.REFLEXIVE.m4ccompleter;
	if (!StringUtils.isEmpty(m4c))
	{

		if(m4c=="Y")
		{
			writer = new StringWriter()
			xml = new MarkupBuilder(writer)
			xml.TreatmentDesc(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, m4ccompleter);
			result.add(writer.toString());
		}	
	}
	return result;
}

/**
 * Generates OLifEExtension section of ACORD 1.0.3 document.
 * Gets called by the CaseToNbMessageAdapter.getAccountInfoExtension
 * 
 * @return
 */
def List<String> getAcctInfoExtension()
{
	def result = [];
	writer = new StringWriter();
	
	if (currentCase.getStatus()!=null)
	{
		if (currentCase.getStatus().equals(CASE_STATUS.APPROVED) ||
			currentCase.getStatus().equals(CASE_STATUS.DECLINED) ||
			currentCase.getStatus().equals(CASE_STATUS.POSTPONED) ||
			currentCase.getStatus().equals(CASE_STATUS.CLOSED))
		{
			xml = new MarkupBuilder(writer);
			xml.DecisionDate (xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, uwDecisionDate);
			result.add(writer.toString());
			writer.getBuffer().setLength(0);
		}

		if (getUndoFinalActionCounter()>0)
		{
			xml5 = new MarkupBuilder(writer);
			xml5.ReissueInd(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, tc:"1", "TRUE");
			result.add(writer.toString());
			writer.getBuffer().setLength(0);
			xml6 = new MarkupBuilder(writer);
			xml6.ReissueDate(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, currentCase.getProposedPolicyDate());
			result.add(writer.toString());
			writer.getBuffer().setLength(0);
		}

		xml3 = new MarkupBuilder(writer);
		if (StringUtils.isEmpty(uwQuickDecisionInd) || "0".equals(uwQuickDecisionInd))
		{
			xml3.QuickDecisionInd(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, tc:"0", "FALSE");
		}
		else
		{
			xml3.QuickDecisionInd(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, tc:"1", "TRUE");
		}	
		result.add(writer.toString());
		writer.getBuffer().setLength(0);
		if (currentCase.getStatus().equals(CASE_STATUS.APPROVED) && !StringUtils.isEmpty(uwRiskCalcStatusReason))
		{	
			lookup = findInLookup("UW_APPROVED_REASON", uwRiskCalcStatusReason);
			xml1 = new MarkupBuilder(writer);
			xml1.SpecialOfferCode(tc:uwRiskCalcStatusReason, xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, StringUtils.upperCase(lookup.description));
			result.add(writer.toString());
			writer.getBuffer().setLength(0);
		}
		else if (currentCase.getStatus().equals(CASE_STATUS.DECLINED) ||
				currentCase.getStatus().equals(CASE_STATUS.POSTPONED) ||
				currentCase.getStatus().equals(CASE_STATUS.CLOSED))
		{
			if (!StringUtils.isEmpty(uwRiskCalcStatusReason))
			{
				String lookupType = currentCase.getStatus().equals(CASE_STATUS.CLOSED) ? "UW_CLOSEDOUT_REASON":"UW_DECLINE_REASON";
				lookup = findInLookup(lookupType, uwRiskCalcStatusReason);
				xml1 = new MarkupBuilder(writer);
				xml1.DecisionReason(tc:uwRiskCalcStatusReason, xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, StringUtils.upperCase(lookup.description));
				result.add(writer.toString());
				writer.getBuffer().setLength(0);
			}
		}
	}
	faceAmount = 0;
	for (ApplicationQuote quote: currentCase.getApplicationQuotes())
	{
		int seqNum = 0;
		
		if (QUOTE_TYPE.NB.equals(quote.getQuoteType()))
		{
			if (quote.quoteSeqId > seqNum)
			{
				seqNum = quote.quoteSeqId;
				for (ApplicationQuoteItem item: quote.getApplicationQuoteItems())
				{
					if ("FA".equalsIgnoreCase(item.getProductPlanAvailability().getPlanOption().getOptionCode()))
					{
						faceAmount = item.coverageAmount.setScale(0, BigDecimal.ROUND_FLOOR);
						break;
					}
				}
			}
		}
	}
	xml2 = new MarkupBuilder(writer);
	xml2.RequestedFaceAmt(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, faceAmount);
	result.add(writer.toString());
	writer.getBuffer().setLength(0);

	String ownerInd = eformsAnswers.PERSON.oth_owner_ind;
	if (!StringUtils.isEmpty(ownerInd) && ownerInd=="Y")
	{
		xml4 = new MarkupBuilder(writer);
		xml4.OwnerFormNum(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, "AIESUPP-09");
		result.add(writer.toString());
		writer.getBuffer().setLength(0);
	}
	return result;
}

def Lookup findInLookup(String lookupType, String lookupValue)
{
	logger.debug(lookupValue)
	for(Lookup it: lookups)
	{
		if(it.name.equalsIgnoreCase(lookupType) && (lookupValue.equalsIgnoreCase(it.code) || lookupValue.equalsIgnoreCase(it.value))) return it
	}
	return new Lookup(code:"", name:"", value:"")
	
}

def Boolean isUndoFinalActionApplicable()
{
//	return Boolean.valueOf(getUndoFinalActionCounter()<1);
	return true;
}

def int getUndoFinalActionCounter()
{
	if (currentCase.getPolicyNoSuffix() != null)
	{
		String undoFinalActionCounterStr = currentCase.getPolicyNoSuffix();
		return Integer.parseInt(undoFinalActionCounterStr);
	}
	return 0;
}

def applyUndoFinalAction()
{
	int undoFinalActionCounter = getUndoFinalActionCounter();
	undoFinalActionCounter++;
	currentCase.setPolicyNoSuffix(String.valueOf(undoFinalActionCounter));
	CaseUser svcCaseUser = currentCase.getCaseUser(SecurityOu.OU_CLASS.SVC);
	if(svcCaseUser != null)
	{
		svcCaseUser.setUserId(null);
	}
}

def String getPolicyStatus()
{
	return currentCase.getStatus().name();
}

def String getUWName()
{
	String uwName = "";
	CASE_STATUS caseStatus = currentCase.getStatus();
	if (caseStatus.equals(CASE_STATUS.APPROVED) || caseStatus.equals(CASE_STATUS.DECLINED) || caseStatus.equals(CASE_STATUS.CLOSED) || caseStatus.equals(CASE_STATUS.POSTPONED))
	{
		uwName = currentCase.getUwOwner().getUsername();		
	}
	else if (uwStatus.equals(UW_CASE_STATUS.INCOMPLETE) && CASE_STATUS.AUTODECLINE.equals(currentCase.getStatus()))
	{
		uwName = "AUTODECL";
	}
	return uwName;
}

/**
 * Hook for getting the Home Office number assigned to the application form (HOAppFormNumber)
 * Usually this is a constant value defined in the NB mapping, but on occasions it varies.
 * 
 * In this case the HOAppFormNumber is defined diffirently for non-compact states.
 * Within those non-compact states there is also some variation in the format.
 */
def String getHOAppFormNumber()
{
	//notice that the string variable HOAppFormNumber is 'injected' into scope just like the variable currentCase
	String retVal=null;
	String contractLocale=currentCase.getContractLocale();
	cutOffProperty = SystemConfiguration.current().getCarrierProperty("GLIC", "cutOffDate")
	Date cuttOffDate=DateUtil.parseDate(DateUtil.FORMAT_SLASH_DATE, cutOffProperty);
		if(currentCase.getCreatedDate().before(cuttOffDate)){
		boolean isNonCompactState= ["AZ","AR","DC","CA","CT","DE","FL","MT","NY","ND","SD"].contains(contractLocale);
		if( isNonCompactState ){
			if( ["AR","DE"].contains(contractLocale) ){
				retVal="AWLTL-12";
			}else if (["AZ","CA","CT","NY"].contains(contractLocale)){
				retVal="AWLTL-12-"+contractLocale;
			}else if (["DC","MT","FL"].contains(contractLocale)){
				retVal="AWLTL-13-"+contractLocale;

				if("FL".equalsIgnoreCase(contractLocale)){
					if (currentCase.getCaseSource()==Case.CASE_SOURCE.EXTERNAL){
						retVal=retVal+"-A"
					}
				}
			}else if(["SD"].contains(contractLocale)){
				retVal="AWLTL-2013"
			}
		}else{
		retVal="ICC12-AWLTL";
	}}
	else if(currentCase.getCreatedDate().after(cuttOffDate)){
		boolean isNonCompactState= ["AZ","DC","CA","CT","DE","FL","MT","NY","ND","SD"].contains(contractLocale);
		if( isNonCompactState ){
			if( ["DE","ND"].contains(contractLocale) ){
				retVal="AWLTL-13"+"(R)";
			}else if (["AZ","CA","CT","NY"].contains(contractLocale)){
				retVal="AWLTL-13-"+contractLocale;
			}else if (["DC","MT","FL"].contains(contractLocale)){
				retVal="AWLTL-13-"+contractLocale+"(R)";

				if("FL".equalsIgnoreCase(contractLocale) || "NY".equalsIgnoreCase(contractLocale)){
					if (currentCase.getCaseSource()==Case.CASE_SOURCE.EXTERNAL){
						retVal=retVal+"-A"
					}
				}
			}else if(["SD"].contains(contractLocale)){
				retVal="AWLTL-13"+"(R)"
			}
		}}
	else{
		retVal=HOAppFormNumber;
	}
	return retVal;
}

def void postSyncRequirementActions()
{
	if (allRequirements != null)
	{
		for (CaseRequirement req : allRequirements)
		{
			System.out.println("CUR REQ "+req.toString());
			if (REQUIREMENT_TYPE.ESIGNATURE.equals(req.getType()) && CASE_REQUIREMENT_TARGET.PRIMARY_INSURED.equals(req.getTarget()) && "ACK".equals(req.getRequirementCode()))
			{
				if(eformsAnswers.APPLICANT_AUTHORIZATION.ack_sig_insured != null && "Y".equals(eformsAnswers.APPLICANT_AUTHORIZATION.ack_sig_insured))
				{
					req.setStatus(STATUS.RESOLVED);
					req.setSignatureStatus(SIGNATURE_STATUS.SIGNED);
					req.setSignatureCity(eformsAnswers.APPLICANT_AUTHORIZATION.ack_city);
					req.setSignatureState(eformsAnswers.APPLICANT_AUTHORIZATION.ack_state);
					req.setSignatureDate(DateUtil.convertDate(eformsAnswers.APPLICANT_AUTHORIZATION.ack_date_insured));
					hibernateTemplate.update(req);
					System.out.println("HFA_PAPER_APP.ack_sig_insured!!!!!!!!!!!");
				}
			}else if(REQUIREMENT_TYPE.ESIGNATURE.equals(req.getType()) && CASE_REQUIREMENT_TARGET.POLICY_OWNER.equals(req.getTarget()) && "ACK".equals(req.getRequirementCode()))
			{
				if(eformsAnswers.APPLICANT_AUTHORIZATION.ack_sig_owner != null && "Y".equals(eformsAnswers.APPLICANT_AUTHORIZATION.ack_sig_owner))
				{
					//ack_sig_owner
					req.setStatus(STATUS.RESOLVED);
					req.setSignatureStatus(SIGNATURE_STATUS.SIGNED);
					req.setSignatureCity(eformsAnswers.APPLICANT_AUTHORIZATION.ack_city);
					req.setSignatureState(eformsAnswers.APPLICANT_AUTHORIZATION.ack_state);
					req.setSignatureDate(DateUtil.convertDate(eformsAnswers.APPLICANT_AUTHORIZATION.ack_date_owner));
					hibernateTemplate.update(req);
					System.out.println("HFA_PAPER_APP.ack_sig_owner!!!!!!!!!!!");
				}
			}else if(REQUIREMENT_TYPE.ESIGNATURE.equals(req.getType()) && CASE_REQUIREMENT_TARGET.AGENT.equals(req.getTarget()) && "AAT".equals(req.getRequirementCode()))
			{
				if(eformsAnswers.AGENT_ATTESTATION != null && "Y".equals(eformsAnswers.AGENT_ATTESTATION.attest_ind))
				{
					//ack_sig_owner
					req.setStatus(STATUS.RESOLVED);
					req.setSignatureStatus(SIGNATURE_STATUS.SIGNED);
					req.setSignatureDate(DateUtil.convertDate(eformsAnswers.AGENT_ATTESTATION.agent_date));
					hibernateTemplate.update(req);
					System.out.println("HFA_PAPER_APP.agent_sig!!!!!!!!!!!");
				}
			}
		}
	}
}
def getNextSequenceNumber()
{
	CoreServices core = coreServices;
	SequenceService seqs = core.getSequenceService();
	String seqNumber = seqs.getNextSequenceNo(2, SEQUENCE_TYPE.APPOINTMENT_NO);
	seqNumber=seqNumber.padLeft(8,"0")
	return seqNumber;
}

def BigDecimal performWOPCalculation()
{
	BigDecimal newPremiumAmt = premiumAmt;
	return newPremiumAmt;
}

def Boolean getSupplementalPdfContent() {
	boolean result = false
	
	result = addSupplementalInformation() || result
	result = addAmendmentInformation() || result 
	
	return result
}

def Boolean addSupplementalInformation() {
	def pdfElements = []

	def insuredName = currentCase.primaryApplicant.firstName + " " + (currentCase.primaryApplicant.middleName?:'') + " " + currentCase.primaryApplicant.lastName
	def insuredType = APPLICANT_TYPE.PRIMARY_INSURED.name()
	def pdfsToAdd = addMericaStatements()
	if (pdfsToAdd) {
		pdfElements.addAll(pdfsToAdd)
		addSupplementalInformationHeader(insuredName, insuredType)
		pdfsToAdd.each { pdfDocument.add(it) }
	}

	return !pdfElements.empty
}

def Boolean addSupplementalInformationHeader(String fullName, String insuredType) {
	pdfDocument.newPage()
	pdfDocument.add(AbstractItextReport.sectionHeader("Medical and Background Supplemental Detail"))

	p = new Paragraph('\n', PdfUtil.HELVETICA_9_BOLD)
	p.add(String.format("PROPOSED INSURED: %1\$-70sPOLICY NO: %2\$-10s  ", fullName, (currentCase.policyNo ?: "Not available")) + '\n')
	pdfDocument.add(p)

	return Boolean.TRUE
}

def List<Element> addMericaStatements() {
	def pdfElements = []

	if (STATEMENTS) {
		def p = new Paragraph('', PdfUtil.HELVETICA_8)
		def insuredType = com.stepsoln.core.db.enums.APPLICANT_TYPE.PRIMARY_INSURED.name()
		p.add(caseHelper.getMericaStatementsTextBlock(STATEMENTS[insuredType]))
		pdfElements.add(p)
	}

	return pdfElements
}

def Boolean addAmendmentInformation() {
	def questions = [
		'oth_owner_ind',
		'm1a',
		'm1b',
		'm1c',
		'm1d',
		'm1e',
		'm1f',
		'm1g',
		'm1h',
		'm1i',
		'm1j',
		'm2',
		'm4a',
		'm4d',
		'm4e',
		'm4f',
		'm4g',
		'm5b',
		'm7',
		'm8',
		'm9a'
	]
	boolean answeredY = questions.find { EFORMS.REFLEXIVE[it] == 'Y' }
	if ("NY" == currentCase.getContractLocale() &&
		(answeredY 
		|| ('Y' == EFORMS.REFLEXIVE['weight_chg_ind'] && 'loss'.equalsIgnoreCase(EFORMS.REFLEXIVE['weight_gain_loss']))
		|| '0' == EFORMS.PERSON['smoker_ind']))
	{
		generateNYAmendmentContent(EFORMS.REFLEXIVE)
		return true
	}
	else if ("NY" != currentCase.getContractLocale())
	{
		def pdfElements = processReflexiveQuestions(QUESTIONS.REFLEXIVE, EFORMS.REFLEXIVE)

		pdfDocument.newPage()
		pdfElements.each { it -> pdfDocument.add(it) }

		return !pdfElements.isEmpty()
	}
	return false
}

def generateNYAmendmentContent(Map<String,String> reflexives) throws Exception
{
	pdfDocument.newPage()
	Paragraph p = new Paragraph();
	p.setFont(PdfUtil.HELVETICA_16);
	p.add("Gerber Life Insurance Company");
	p.setFont(PdfUtil.HELVETICA_12);
	p.add("\n1311 Mamaroneck Avenue");
	p.add("\nWhite Plains, New York 10605");
	p.setFont(PdfUtil.HELVETICA_16);
	p.add("\n\nAmendment of Application");
	p.setAlignment(Element.ALIGN_CENTER);
	pdfDocument.add(p);

	p = new Paragraph("\n", PdfUtil.HELVETICA_10);
	p.add("Policy number: " + currentCase.policyNo);
	p.add("\nProposed Insured: " + [
		currentCase.primaryApplicant.firstName,
		currentCase.primaryApplicant.middleName,
		currentCase.primaryApplicant.lastName
	].findAll().join(" "));
	pdfDocument.add(p);

	p = new Paragraph("\n", PdfUtil.HELVETICA_12);
	p.add("On the submitted Application to Gerber Life Insurance Company, the answers to ");
	p.add("\nthe following questions are amended and supplemented as follows:");
	pdfDocument.add(p);

	if ('Y' == EFORMS.PERSON['oth_owner_ind'])
	{
		pdfDocument.add(createSectionIOwnership());
	}
	if ('Y' == EFORMS.REFLEXIVE['weight_chg_ind'] && 'loss'.equalsIgnoreCase(EFORMS.REFLEXIVE['weight_gain_loss']))
	{
		pdfDocument.add(createSectionIIIWeightLoss());
	}
	if ('0' == EFORMS.PERSON['smoker_ind'])
	{
		pdfDocument.add(createSectionIIITobacco());
	}

	createSectionIIIQ1aTo1jAnd4a().each { pdfDocument.add(it) }

	if (EFORMS.REFLEXIVE['family_disorder___0'])
	{
		pdfDocument.add(createSectionIIIQuestion2());
	}
	if ('Y' == EFORMS.REFLEXIVE['m4d'] || 'Y' == EFORMS.REFLEXIVE['m4e'])
	{
		createSectionIIIQuestion4d4e().each { pdfDocument.add(it) }
	}
	if ('Y' == EFORMS.REFLEXIVE['m4f'] || 'Y' == EFORMS.REFLEXIVE['m4g'])
	{
		createSectionIIIQuestion4f4g().each { pdfDocument.add(it) }
	}
	if (0 == currentCase.primaryApplicant.gender && 'Y' == EFORMS.REFLEXIVE['m5b'])
	{
		pdfDocument.add(createSectionIIIQuestion5b())
	}
	if ('Y' == EFORMS.REFLEXIVE['m7'])
	{
		pdfDocument.add(createSectionIIIQuestion7());
	}
	if ('Y' == EFORMS.REFLEXIVE['m8'])
	{
		pdfDocument.add(createSectionIIIQuestion8());
	}
	if ('Y' == EFORMS.REFLEXIVE['m9a'])
	{
		pdfDocument.add(createSectionIIIQuestion9a());
	}
	if ('Y' == EFORMS.REFLEXIVE['m9b'])
	{
		pdfDocument.add(createSectionIIIQuestion9b());
	}

	p = new Paragraph();
	p.setFont(PdfUtil.HELVETICA_10);
	p.add("\nIt is hereby represented and agreed that, to the best of my knowledge and belief:");
	p.add("\n      *   the  application,  including  this  amendment,  shall  form  the  basis  and");
	p.add("\n          become part of the policy applied for, and,");
	p.add("\n      *   the  above  statement(s)  and  all  other  responses  within  all  parts  of  the");
	p.add("\n          application  continue to be complete and true.");
	pdfDocument.add(p);

	String primarySig = caseHelper.insertSignature("ACK", "PRIMARY_INSURED")
	String ownerSig = caseHelper.insertSignature("ACK", "POLICY_OWNER")
	def signature = primarySig && SIGNATURES.get(primarySig) instanceof Image ? Image.getInstance(SIGNATURES.get(primarySig)) : SIGNATURES.get(primarySig)

	float leading = 8f
	p = new Paragraph("\nSignature of Proposed Insured: ", PdfUtil.HELVETICA_10);
	p.setLeading(leading)
	p.setFont(PdfUtil.HELVETICA_10_UNDERLINE);
	p.add(inputField(80, (signature instanceof java.lang.String ? signature : '')));
	pdfDocument.add(p)

	float x = 190f;
	float y = pdfWriter.getVerticalPosition(false)
	addImageSignature(signature, x, y)

	String signatureDate = caseHelper.getSignatureDate("ACK", "PRIMARY_INSURED") ?: ''
	p = new Paragraph("\nDate: ", PdfUtil.HELVETICA_10);
	p.setLeading(leading)
	p.setFont(PdfUtil.HELVETICA_10_UNDERLINE);
	p.add(inputField(80, signatureDate));
	pdfDocument.add(p)

	signature = ownerSig && SIGNATURES.get(ownerSig) instanceof Image ? Image.getInstance(SIGNATURES.get(ownerSig)) : SIGNATURES.get(ownerSig)
	p = new Paragraph("\nSignature of Policyowner (if other than Proposed Insured): ", PdfUtil.HELVETICA_10);
			p.setLeading(leading)
	p.setFont(PdfUtil.HELVETICA_10_UNDERLINE);
	p.add(inputField(80, (signature instanceof java.lang.String ? signature : '')));
	pdfDocument.add(p)

	x = 310f;
	y = pdfWriter.getVerticalPosition(false)
	addImageSignature(signature, x, y)
}

def addImageSignature(signature, x, y)
{
	if (signature instanceof Image)
	{
		signature.scaleToFit(60f, 15f)
		signature.setAbsolutePosition(x, y)
		pdfDocument.add(signature)
	}
}

def Element createSectionIIIQuestion9b()
{
	Map<String, String> followupMap = EFORMS.REFLEXIVE.getQuestions()
	Map<String, String> answerMap = EFORMS.REFLEXIVE.getAnswerMap()
	def m9bMap = new HashMap<String, String>()
	EFORMS.REFLEXIVE.keySet().each {
		if (it.contains('m9bcompleter') && !it.contains('followup'))
		{
			m9bMap.put(EFORMS.REFLEXIVE[it], it)
		}
	}

	Paragraph p = new Paragraph("\nSection III, Question 9b", PdfUtil.HELVETICA_12);
	p.setFont(PdfUtil.HELVETICA_10);
	p.add("\nPlease indicate the type of avocation:\n");
	valueLabel(p, 3, markX(m9bMap['SCUBA DIVING']? true: false), ' Scuba Diving   ')
	valueLabel(p, 3, markX(m9bMap['SKIN DIVING']? true: false), ' Sking Diving   ')
	valueLabel(p, 3, markX(m9bMap['SKY DIVING']? true: false), ' Sky Diving   ')
	valueLabel(p, 3, markX(m9bMap['PARACHUTING']? true: false), ' Parachuting   ')
	valueLabel(p, 3, markX(m9bMap['HANG GLIDING']? true: false), ' Hang Gliding   ')
	valueLabel(p, 3, markX(m9bMap['BALLOONING']? true: false), ' Ballooning   ')
	valueLabel(p, 3, markX(m9bMap['BUNGEE JUMPING']? true: false), ' Bungee Jumping   ')
	valueLabel(p, 3, markX(m9bMap['MOTORCYCLE RACING']? true: false), ' Motorcycle Racing   ')
	valueLabel(p, 3, markX(m9bMap['MOTORCAR RACING']? true: false), ' Motorcar Racing   ')
	valueLabel(p, 3, markX(m9bMap['MOUNTAIN CLIMBING']? true: false), ' Mountain Climbing   ')
	valueLabel(p, 3, markX(m9bMap['CAVE EXPLORATION']? true: false), ' Cave Exploration   ')
	valueLabel(p, 3, markX(m9bMap['RODEO']? true: false), 'Rodeo')

	p.add('\n')
	m9bMap.sort().each { activity, questionId ->
		p.add("\nIf ");
		p.setFont(PdfUtil.HELVETICA_10_BOLD);
		p.add(activity.toString().toLowerCase());
		p.setFont(PdfUtil.HELVETICA_10);
		p.add(" is selected, then complete the following:\n");

		printFollowUpQuestions(p, questionId, followupMap, answerMap)
	}
	p
}

def Element createSectionIIIQuestion9a()
{
	Map<String, String> followupMap = EFORMS.REFLEXIVE.getQuestions()
	Map<String, String> answerMap = EFORMS.REFLEXIVE.getAnswerMap()
	def m9aMap = new HashMap<String, String>()
	EFORMS.REFLEXIVE.keySet().each {
		if (it.contains('m9acompleter') && !it.contains('followup'))
		{
			m9aMap.put(EFORMS.REFLEXIVE[it], it)
		}
	}

	Paragraph p = new Paragraph("\nSection III, Question 9a", PdfUtil.HELVETICA_12);
	p.setFont(PdfUtil.HELVETICA_10);
	p.add("\nPlease indicate the type of pilot:\n");

	boolean isTest = m9aMap['TEST PILOT AIRWORTHINESS'] || m9aMap['TEST PILOT DEVELOPMENT'] || m9aMap['TEST PILOT PRODUCTION'] || m9aMap['TEST PILOT PROTOTYPE']

	valueLabel(p, 3, m9aMap['COMMERCIAL PILOT']?'X':'', 'Commercial   ')
	valueLabel(p, 3, m9aMap['INSTRUCTOR PILOT']?'X':'', 'Instructor   ')
	valueLabel(p, 3, m9aMap['MILITARY PILOT']?'X':'', 'Military   ')
	valueLabel(p, 3, m9aMap['PRIVATE PILOT']?'X':'', 'Private   ')
	valueLabel(p, 3, m9aMap['STUDENT PILOT']?'X':'', 'Student   ')
	valueLabel(p, 3, isTest?'X':'', 'Test')

	p.add('\n')
	m9aMap.sort().each { activity, questionId ->
		p.add("\nIf ");
		p.setFont(PdfUtil.HELVETICA_10_BOLD);
		p.add(activity.toString().toLowerCase());
		p.setFont(PdfUtil.HELVETICA_10);
		p.add(" is selected, then complete the following:\n");

		printFollowUpQuestions(p, questionId, followupMap, answerMap)
	}
	p
}

def printFollowUpQuestions(Paragraph p, String questionId, Map followupMap, Map answerMap)
{
	followupMap.sort().each { k, v ->
		if (k.indexOf(questionId+'_followup_question_') >= 0)
		{
			Object a = EFORMS.REFLEXIVE[k]
			if(a)
			{
				if (a.toString().isNumber())
				{
					a = stringToMap(answerMap[k]) ? stringToMap(answerMap[k])[a] : a
				}
				else if (a instanceof Object[])
				{
					def l = []
					a.each{ entry ->
						l.add(stringToMap(answerMap[k])[entry])
					}
					a = l.findAll().join(', ')
				}
				labelValue(p, a ? 1 : 25, v, a? a.toString():'')
				p.add('\n')
			}
		}
	}
}
def Element createSectionIIIQuestion8()
{
	Paragraph p = new Paragraph("\nSection III, Question 8", PdfUtil.HELVETICA_12);
	labelValue(p, 12, '\nDate of Enlistment:   ', EFORMS.REFLEXIVE['enrollment_date'])
	p.add(' (mm/dd/yy)')
	def branches = [
		'Army',
		'Navy',
		'Air Force',
		'Marines'
	]
	p.setFont(PdfUtil.HELVETICA_10)
	p.add("\nBranch of Military Service: ")
	for(b in branches)
	{
		def mark = ''
		if (b.trim() == EFORMS.REFLEXIVE['military_branch'])
		{
			mark = 'X'
		}
		valueLabel(p, 3, mark, ' ' + b + '  ')
	}

	p.add("\nAre you currently deployed overseas or will you be deployed overseas in the next 12 months?  ");
	valueLabel(p, 3, 'Y' == EFORMS.REFLEXIVE['deployed_overseas'] ? 'X' : '', ' Yes  ')
	valueLabel(p, 3, 'N' == EFORMS.REFLEXIVE['deployed_overseas'] ? 'X' : '', ' No')

	return p;
}

def Element createSectionIIIQuestion7()
{
	Paragraph p = new Paragraph("\nSection III, Question 7", PdfUtil.HELVETICA_12);

	def countries = []
	countries.add(EFORMS.REFLEXIVE['m7completer'])
	int i = 0
	while(EFORMS.REFLEXIVE['m7completer1___' + i])
	{
		countries.add(EFORMS.REFLEXIVE['m7completer1___' + i])
		i++
	}

	labelValue(p, 50, '\nPlease indicate what countries you will be visiting: ', countries.findAll().join(', '))
	return p;
}

def Element createSectionIIIQuestion5b()
{
	Paragraph p = new Paragraph('\nSection III, Question 5b', PdfUtil.HELVETICA_12);
	labelValue(p, 12, '\nExpected Delivery Date:  ', EFORMS.REFLEXIVE['m5bcompleter'])
	p.add(' (mm/dd/yy)')
	return p;
}

def List<Element> createSectionIIIQuestion4f4g()
{
	def answers = []
	Paragraph p = new Paragraph("\nSection III, Question 4f, and/or 4g:", PdfUtil.HELVETICA_12);
	answers.add(p)

	if ('Y' == EFORMS.REFLEXIVE['m4f'])
	{
		p = new Paragraph('', PdfUtil.HELVETICA_10)
		labelValue(p, 30, 'Benefit: ', EFORMS.REFLEXIVE['m4fcompleter'])
		answers.add(p)
	}
	if ('Y' == EFORMS.REFLEXIVE['m4g'])
	{
		p = new Paragraph('', PdfUtil.HELVETICA_10)
		labelValue(p, 50, '\nType of Policy: ', EFORMS.REFLEXIVE['m4g_type_of_policy'])
		labelValue(p, 12, '\nDate of application or receipt of benefits: ', EFORMS.REFLEXIVE['m4g_date_of_app_or_benefits'])
		p.add(' (mm/dd/yy)')
		answers.add(p)
	}
	answers
}

def List<Element> createSectionIIIQuestion4d4e()
{
	def answers = []
	Paragraph p = new Paragraph("\nSection III, Question 4d, and/or 4e:", PdfUtil.HELVETICA_12);
	answers.add(p)
	def questions = ['m4d', 'm4e']
	int i = 0
	if (EFORMS.REFLEXIVE['m4d'] == 'Y')
	{
		p = new Paragraph('', PdfUtil.HELVETICA_10);
		def ttp = [
			EFORMS.REFLEXIVE['m4dcompleter'],
			EFORMS.REFLEXIVE['m4d_treatment_details']
		]
		labelValue(p, 50, '\nType of Treatment, Test or Procedure: ', ttp.findAll().join(", "))
		p.add("\nDate of Treatment, Test or Procedure: ");
		if (EFORMS.REFLEXIVE['m4d_date_of_treatment'])
		{
			valueLabel(p, 12, EFORMS.REFLEXIVE['m4d_date_of_treatment'], ' (mm/dd/yy)')
		}
		labelValue(p, 50, '\nFor what condition is this test being performed? ', EFORMS.REFLEXIVE['m4d_condition_treated']?:'')

		answers.add(p)
	}
	if (EFORMS.REFLEXIVE['m4e'] == 'Y')
	{
		Map<String, String> followupMap = EFORMS.REFLEXIVE.getQuestions()
		Map<String, String> answerMap = EFORMS.REFLEXIVE.getAnswerMap()
		p = new Paragraph('', PdfUtil.HELVETICA_10);
		labelValue(p, 50, '\nPlease enter the examination or laboratory test: ', EFORMS.REFLEXIVE['m4ecompleter']?:'')
		p.add('\n')
		printFollowUpQuestions(p, 'm4ecompleter', followupMap, answerMap)

		answers.add(p)
		i++
	}
	answers
}

def Element createSectionIIIQuestion2()
{
	Paragraph p = new Paragraph("\nSection III, Question 2", PdfUtil.HELVETICA_12);
	p.setFont(PdfUtil.HELVETICA_10);
	p.add("\nPlease indicate which of the following relatives were affected:");

	PdfPTable table = new PdfPTable(3);
	PdfPCell c1 = new PdfPCell(new Phrase("Relative", PdfUtil.HELVETICA_10));
	c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	table.addCell(c1);

	c1 = new PdfPCell(new Phrase("Condition", PdfUtil.HELVETICA_10));
	c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	table.addCell(c1);

	c1 = new PdfPCell(new Phrase("Age diagnosed or Age at death", PdfUtil.HELVETICA_10));
	c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	table.addCell(c1);
	table.setHeaderRows(1);

	int i = 0
	while (EFORMS.REFLEXIVE['family_disorder___' + i])
	{
		table.addCell(valueCell(EFORMS.REFLEXIVE['family_relation___' + i]));
		table.addCell(valueCell(EFORMS.REFLEXIVE['family_status___' + i]+ ' - ' + EFORMS.REFLEXIVE['family_disorder___' + i]));
		table.addCell(valueCell(EFORMS.REFLEXIVE['family_age_of_death___' + i]));
		i++;
	}
	p.add(table);

	return p;
}

private static Phrase valueCell(String s)
{
	return new Phrase(s, PdfUtil.HELVETICA_10);
}

def List<Element> createSectionIIIQ1aTo1jAnd4a()
{
	def questions = [
		'm1a',
		'm1b',
		'm1c',
		'm1d',
		'm1e',
		'm1f',
		'm1g',
		'm1h',
		'm1i',
		'm1j',
		'm4a'
	]
	def answeredY = questions.find { EFORMS.REFLEXIVE[it] == 'Y' }
	def answers = []
	Paragraph p = new Paragraph("\nSection III, Questions 1a, 1b, 1c, 1d, 1e, 1f, 1g, 1h, 1i, 1j and/or 4a:", PdfUtil.HELVETICA_12);
	if (answeredY)
	{
		answers.add(p)
	}
	
	int i = 0
	Map<String, String> followupMap = EFORMS.REFLEXIVE.getQuestions()
	Map<String, String> answerMap = EFORMS.REFLEXIVE.getAnswerMap()
	while (questions[i])
	{
		if ('Y' == EFORMS.REFLEXIVE[questions[i]])
		{
			p = new Paragraph()
			p.setFont(PdfUtil.HELVETICA_10)
			String questionId = questions[i] + 'completer'
			labelValue(p, 1, '\nPlease specify the condition:  ', EFORMS.REFLEXIVE[questionId])
			p.add('\n')
			
			printFollowUpQuestions(p, questionId, followupMap, answerMap)

			answers.add(p)
		}
		i++
	}
	return answers
}

def Map stringToMap(String answerSet)
{
	def map = [:]
	if (answerSet && (answerSet.indexOf('|')>=0 || answerSet.indexOf(':') >=0))
	{
		for (String s: answerSet.tokenize('|'))
		{
			map.put(s.substring(0, s.indexOf(':')) , s.substring(s.indexOf(':')+1))
		}
	}
	map
}


def Element createSectionIIITobacco()
{
	Paragraph p = new Paragraph("\nSection III, Tobacco:", PdfUtil.HELVETICA_12);
	p.setFont(PdfUtil.HELVETICA_10)
	p.add("\nWhen was the last time you used any tobacco?");
	valueLabel(p, 15, String.valueOf(EFORMS.PERSON['last_tobacco_mm'])?:'', ' Month  ')
	valueLabel(p, 15, String.valueOf(EFORMS.PERSON['last_tobacco_yy'])?:'', ' Year')
	return p;
}

def Element createSectionIIIWeightLoss() throws IOException, DocumentException
{
	Paragraph p = new Paragraph("\nSection III, Weight Loss:", PdfUtil.HELVETICA_12);
	p.setFont(PdfUtil.HELVETICA_10);
	p.add("\nPlease select the reason for the weight loss:\n");
	valueLabel(p, 3, 'LIFESTYLE_WC' == EFORMS.REFLEXIVE['weightchg_reasons'] ? 'X' : '', ' Change in Lifestyle  ')
	valueLabel(p, 3, 'PROGRAM_WC' == EFORMS.REFLEXIVE['weightchg_reasons'] ? 'X' : '', ' Diet/exercise program  ')
	valueLabel(p, 3, 'PREGNANCY_WC' == EFORMS.REFLEXIVE['weightchg_reasons'] ? 'X' : '', ' Pregnancy\n')
	valueLabel(p, 3, 'EATING_WC' == EFORMS.REFLEXIVE['weightchg_reasons'] ? 'X' : '', ' Change in eating habits  ')
	valueLabel(p, 3, 'MEDICAL_WC' == EFORMS.REFLEXIVE['weightchg_reasons'] ? 'X' : '', ' Medical condition\n')
	valueLabel(p, 3, 'OTHER_WC' == EFORMS.REFLEXIVE['weightchg_reasons'] ? 'X' : '', ' Other')
	labelValue(p, 50, '   Specify', '')

	return p;
}

def Element createSectionIOwnership()
{
	Paragraph p = new Paragraph("\nSection I, Ownership:", PdfUtil.HELVETICA_12);
	labelValue(p, 30, '\nName of Owner:  Last ', EFORMS.OWNER_AUTH['ins_last_name'])
	labelValue(p, 30, '  First ', EFORMS.OWNER_AUTH['ins_first_name'])
	labelValue(p, 10, '  M.I. ', EFORMS.OWNER_AUTH['ins_middle_name'] ? EFORMS.OWNER_AUTH['ins_middle_name'].charAt(0):'')
	labelValue(p, 12, '\nOwners Date of Birth (if trust, date created):  ', EFORMS.OWNER_AUTH['owner_date_of_birth']?:'')
	labelValue(p, 50, '\nRelationship of Owner to Insured:  ', EFORMS.OWNER_AUTH['owner_relationship']?:'')
	labelValue(p, 100, '\nLegal Residence of Owner:  ', EFORMS.OWNER_AUTH['owner_residence_address']?:'')
	labelValue(p, 40, '\nCity:  ', EFORMS.OWNER_AUTH['owner_city']?:'')
	labelValue(p, 20, '  State: ', EFORMS.OWNER_AUTH['owner_residence_state']?:'')
	labelValue(p, 20, '  Zip code: ', EFORMS.OWNER_AUTH['owner_zip']?:'')
	p.setFont(PdfUtil.HELVETICA_10)
	p.add("\nHas the Proposed Insured or Policyowner entered into or made plans to enter into")
	p.add("\na contract, agreement or arrangement to sell, transfer or assign the ownership of or")
	p.add("\na beneficial interest in the policy being applied for to an unrelated third party?")
	valueLabel(p, 3, markX('Y' == EFORMS.OWNER_AUTH['other_owner']), ' Yes   ')
	valueLabel(p, 3, markX('N' == EFORMS.OWNER_AUTH['other_owner']), ' No')
	labelValue(p, 100, '\nIf yes, please identify:  ', EFORMS.OWNER_AUTH['other_owner_completer']?:'')

	return p
}

def List<String> getSelected(Object str)
{
	def l = []
	str.findAll{ if (it.trim())  l.add(it) }
	return l
}

def String inputField(Integer l, String s)
{
	return String.format(" %1\$-" + l + "s ", s);
}

def valueLabel(Paragraph p, Integer length, String value, String label)
{
	Chunk c = new Chunk(inputField(length, value), PdfUtil.HELVETICA_10_UNDERLINE)
	p.add(c)
	c = new Chunk(label, PdfUtil.HELVETICA_10)
	p.add(c)
}

def labelValue(Paragraph p, Integer length, String label, String value)
{
	Chunk c = new Chunk(label, PdfUtil.HELVETICA_10)
	p.add(c)
	c = new Chunk(inputField(length, value), PdfUtil.HELVETICA_10_UNDERLINE)
	p.add(c)
}

def markX(boolean test)
{
	return test ? 'X':''
}

def processReflexiveQuestions(Map<String, Object> eformQuestionMap, EFormAnswers reflexive) {
	def questionIds = [
		weight_chg_ind:['// Significant weight change']
		, m1a:['m1acompleter','m1acompleter1___']
		, m1b:['m1bcompleter','m1bcompleter1___']
		, m1c:['m1ccompleter','m1ccompleter1___']
		, m1d:['m1dcompleter','m1dcompleter1___']
		, m1e:['m1ecompleter','m1ecompleter1___']
		, m1f:['m1fcompleter','m1fcompleter1___'], m1fCT:['m1fcompleter', 'm1fcompleter1___']
		, m1g:['m1gcompleter','m1gcompleter1___']
		, m1h:['m1hcompleter','m1hcompleter1___']
		, m1i:['m1icompleter','m1icompleter1___']
		, m1j:['m1jcompleter','m1jcompleter1___'], m1jCT:['m1jcompleter','m1jcompleter1___']
		, m2:['// Family history questions']
		, m3:['m3completer','m3completer1___'], m3FL:['m3completer','m3completer1___'], m3CA:['m3completer','m3completer1___'], m3CT:['m3completer','m3completer1___'], m3NY:['m3completer','m3completer1___'], 
		, m4a:['m4acompleter','m4acompleter1___']
		, m4b:['m4bcompleter','m4bcompleter1___']
		, m4c:['m4ccompleter','m4ccompleter1___']
		, m4d:['m4dcompleter','m4d_treatment_details','m4d_date_of_treatment','m4d_condition_treated']
		, m4e:['m4ecompleter','m4ecompleter1___']
		, m4f:['m4fcompleter']
		, m4g:['m4gcompleter']
		, m5a:['m5acompleter', 'm5acompleter1___'], m5aCT:['m5acompleter', 'm5acompleter1___']
		, m5b:['m5bcompleter']
		, m6a:['m6acompleter']
		, m6b:['m6bcompleter'], m6bCT:['m6bcompleter']
		, m7:['// Foreign travel'], m7CA:['// Foreign travel'], m7FL:['// Foreign travel']
		, m8:['military_status','enrollment_date','deployed_overseas','military_branch'], m8CT:['military_status','enrollment_date','deployed_overseas','military_branch'], m8FL:['military_status','enrollment_date','deployed_overseas','military_branch']
		, m9a:['m9acompleter','m9acompleter1___'], m9aFL:['m9acompleter','m9acompleter1___'], m9aCT:['m9acompleter','m9acompleter1___']
		, m9b:['m9bcompleter','m9bcompleter1___'], m9bCT:['m9bcompleter','m9bcompleter1___']
	]
	
	Map<String, String> mericaQuestionMap = reflexive.getQuestions()
	Map<String, String> mericaAnswerMap = reflexive.getAnswerMap()

	def pdfElements = []

	questionIds.each {
		eformKey, eformKeyCompleter ->
		if (reflexive[eformKey] && 'Y'.equalsIgnoreCase(reflexive[eformKey]))
		{
			Paragraph p = new Paragraph('\n')
			labelValue(p, eformQuestionMap[eformKey] ? 1: 25, eformQuestionMap[eformKey], '  Yes  ')
			p.add('\n')
			eformKeyCompleter.each {
				c ->
				if ('weight_chg_ind' == eformKey)
				{
					labelValue(p, 1, 'Weight change in pounds ', reflexive['weight_chg_pounds'])
					p.add('\n')
					labelValue(p, 1, 'Was this change a gain or a loss? ', reflexive['weight_gain_loss'])
					p.add('\n')
					labelValue(p, 1, 'Please specify a reason ', reflexive['weightchg_reasons'])
					p.add('\n')
					if (reflexive['weightchg_reasons'] == "MEDICAL_WC")
					{
						labelValue(p, 1, 'Please specify ', reflexive['medicalwccompleter'])
						p.add('\n')
					}
					else if (reflexive['weightchg_reasons'] == "OTHER_WC")
					{
						labelValue(p, 1, 'Please provide explanation ', reflexive['otherwccompleter'])
						p.add('\n')
					}
				}
				else if ('m2' == eformKey)
				{
					int i = 0
					while (reflexive['family_disorder___'+i])
					{
						labelValue(p, 1, 'Family member\'s disease ', reflexive['family_disorder___'+i])
						p.add('\n')
						labelValue(p, 1, 'Relationship to the Insured ', reflexive['family_relation___'+i])
						p.add('\n')
						def deadOrAlive = reflexive['family_status___'+i]
						def ageLabel = 'Deceased'.equalsIgnoreCase(deadOrAlive) ? 'Age at death ' : 'Age diagnosed '
						labelValue(p, 1, ageLabel, reflexive['family_age_of_death___'+i])
						p.add('\n')
						labelValue(p, 1, 'Family member\'s status ', deadOrAlive)
						p.add('\n')
						i++
						if (reflexive['family_disorder___'+i])
						{
							p.add('\n')
						}
					}
				}
				else if ('m7' == eformKey)
				{
					int i = 0
					String idx = ''
					while (reflexive['m7completer'+idx])
					{
						labelValue(p, 1, 'Please specify the country ', reflexive['m7completer'+idx])
						p.add('\n')
						labelValue(p, 1, 'What is the length of your stay or travel outside of the U.S.A.? ', reflexive['m7length'+idx])
						p.add('\n')
						labelValue(p, 1, 'What is the reason for your travel? ', reflexive['m7reason'+idx])
						p.add('\n')
						idx = "1___" + i
						i++
						if (reflexive['m7completer'+idx])
						{
							p.add('\n')
						}
					}
				}
				else if (c.contains('___')) 
				{
					int i = 0
					String indexedEformKey = c + i
					while (reflexive[indexedEformKey]) 
					{
						labelValue(p, reflexive[indexedEformKey] ? 1: 25, eformQuestionMap[indexedEformKey]?:eformQuestionMap[indexedEformKey.substring(0, indexedEformKey.indexOf('___')-1)]?:'', reflexive[indexedEformKey])
						p.add('\n')
						printFollowUpQuestions(p, indexedEformKey, mericaQuestionMap, mericaAnswerMap)
						i++
						indexedEformKey = c + i
					}
				}
				else if (reflexive[c] && !c.contains('___')) 
				{
					labelValue(p, eformQuestionMap[c] ? 1: 25, eformQuestionMap[c]?:'', reflexive[c])
					p.add('\n')
					printFollowUpQuestions(p, c, mericaQuestionMap, mericaAnswerMap)
				}
			}
			pdfElements.add(p)
		}
	}
	
	pdfElements
}

def customPageBehavior()
{
	DialogButtonConfig b = DialogButtonConfig.create('closeBtn', 'mobileOptInDlg.hide()', 'OK')
	DialogConfig d = DialogConfig.create('mobileOptInScript', 'Can Gerber Life call you at the wireless phone number you provided using an automated telephone dialing system?', 'mobileOptInDlg', [b])
	return ['/pages/case/new_case.xhtml' :
				[
				 UserInterfaceFacesBehavior.createDialog('new_case.xhtml', d)
				 , UserInterfaceFacesBehavior.createJavascript('new_case.xhtml', 'newCaseForm:caseApplicantFields:primaryPhoneType', 'setOnchange', "if (this.value == 'MOBILE') mobileOptInDlg.show()")
				 , UserInterfaceFacesBehavior.createJavascript('new_case.xhtml', 'newCaseForm:caseApplicantFields:secondaryPhoneType', 'setOnchange', "if (this.value == 'MOBILE') mobileOptInDlg.show()")
				]
			]
}


def EmailRequestType generateEmailContentFor(Applicant app)
{
	EmailRequestType emailRequest = null;
	try{
	
	Long agentId = currentCase.getAgentId();
	Agent agent = coreServices.getCaseService().getHibernateTemplate().get(Agent.class, agentId);

	ConfigResource resource = coreServices.getConfigurationRepository().loadLastestResource(currentCase.getCarrierId(), currentCase.getProductId(), "glic_agent_send_case_to_applicant_for_review");
	if(resource != null && app != null && !StringUtils.isEmpty(app.getEmail()))
	{
		emailRequest = new EmailRequestType();
		SecurityInvitation invitation = coreServices.getInvitationService().createSecurityInvitation(currentCase.getCaseId(), app.getId(), "&nextViewName=SecurityInvitation.applicantESignatureFlowStartAction", null, 30);
		String emailContent = TemplateHelper.velocityTranslateString(new String(resource.getContent()), 
			new QuickMap("policyNumber", currentCase.getPolicyNo())
				.add("applicantFirstName",app.getFirstName())
				.add("applicantFirstName",app.getLastName())
				.add("inviteLink",invitation.getInvitationUrl())
				);
		emailRequest.setAddressFrom(agent.getEmail());
		emailRequest.setAddressTo(app.getEmail());
		emailRequest.setBodyContent(emailContent);
		emailRequest.setSubject("Your Life Insurance Application with Gerber Life");
	}
	}catch(Exception e)
	{
		e.printStackTrace();
	}
	return emailRequest;
}

def List generateEmailContentForSendApplicantForReview()
{
	List retList = new ArrayList();
	Applicant primaryApplicant = currentCase.getPrimaryApplicant();
	if(primaryApplicant != null)
	{
		EmailRequestType emailContent = generateEmailContentFor(primaryApplicant);
		if(emailContent != null)
		{
			retList.add(emailContent);
		}		
	}
	
	Applicant policyOwner = currentCase.getApplicant(com.stepsoln.core.db.enums.APPLICANT_TYPE.POLICY_OWNER);
	if(policyOwner != null)
	{
		EmailRequestType emailContent = generateEmailContentFor(policyOwner);
		if(emailContent != null)
		{
			retList.add(emailContent);
		}
	}
	
	return retList;
}

def Boolean isUniSexRates()
{
	if (applicantResidenceState.equalsIgnoreCase("MT"))
	{
		return true;
	}
	return false;
}

def Boolean isValidTobaccoDuration()
{
	Date uwTobaccoLastUseDate = DateUtil.formatMonthYear().parse(applicantTobaccoUsage);
	if (uwTobaccoLastUseDate == null)
	{
		return false;
	}
	int diff = DateUtil.monthsInBetween(new Date(), uwTobaccoLastUseDate);
	if (diff <= 0 && diff >= -36)
	{
		return true;
	}
	return false;
}

