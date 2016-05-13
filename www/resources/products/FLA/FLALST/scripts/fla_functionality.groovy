import com.stepsoln.core.db.security.SecurityUser;

import groovy.xml.MarkupBuilder

import org.apache.camel.ProducerTemplate
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger

import com.itextpdf.text.Element
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.stepsoln.core.db.Lookup
import com.stepsoln.core.db.cases.ApplicationQuote
import com.stepsoln.core.db.cases.ApplicationQuoteItem
import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.db.cases.ApplicationQuote.QUOTE_TYPE
import com.stepsoln.core.db.cases.Case.UW_CASE_STATUS
import com.stepsoln.core.db.product.PlanOption.OPTION_VALUE_TYPE
import com.stepsoln.core.db.security.SecurityOu
import com.stepsoln.core.db.security.SecurityUser
import com.stepsoln.core.db.services.CoreServices
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.generated.nb.PartyType
import com.stepsoln.core.generated.nb.RiskType
import com.stepsoln.core.generated.nb.TXLifeRequestType
import com.stepsoln.core.rules.coverages.CoverageOption
import com.stepsoln.core.rules.coverages.CoveragePlan
import com.stepsoln.core.util.GroovyHelper
import com.stepsoln.core.util.IOUtils
import com.stepsoln.core.util.StringUtil
import com.stepsoln.core.util.modeladapters.NBMessageAdapterUtil
import com.stepsoln.core.util.pdfreports.AbstractItextReport
import com.stepsoln.servicebus.api.XMLParseHelper

def BigDecimal premiumAmt;
def List<CoveragePlan> coveragePlans;
def Case currentCase;
def Logger logger;
def Map<String, Map<String, Object>> eformsAnswers;
def List<Lookup> lookups;
def String uwRiskCalcStatusReason;
def String uwDecisionDate;
def String uwQuickDecisionInd;
def Services services;
def CoreServices coreServices;
def ProducerTemplate producerTemplate;
def SecurityUser securityUser;
def List<SecurityOu> securityOu;

def BigDecimal performWOPCalculation()
{
	BigDecimal newPremiumAmt = premiumAmt;
	for (CoveragePlan coveragePlan : coveragePlans)
	{
		for (CoverageOption option : coveragePlan.getCoverageOptions())
		{
			if ((option.isSelected()) && ("LSTWP".equalsIgnoreCase(option.getOptionCode())))
			{
				newPremiumAmt = newPremiumAmt + 0.2195122 * newPremiumAmt;
			}
		}
	}
	return newPremiumAmt;
}

def String getPolicyStatus()
{
	return currentCase.getStatus().name();
}


def List<String> getAcctInfoExtension()
{
	def result = [];
	writer = new StringWriter();
	
	if (currentCase.getUwCaseStatus()!=null)
	{
		if (currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.APPROVED) ||
			currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.DECLINED) ||
			currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.POSTPONED) ||
			currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.CLOSED))
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
		if (currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.APPROVED) && !StringUtils.isEmpty(uwRiskCalcStatusReason))
		{
			lookup = findInLookup("UW_APPROVED_REASON", uwRiskCalcStatusReason);
			xml1 = new MarkupBuilder(writer);
			xml1.SpecialOfferCode(tc:uwRiskCalcStatusReason, xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, StringUtils.upperCase(lookup.description));
			result.add(writer.toString());
			writer.getBuffer().setLength(0);
		}
		else if (currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.DECLINED) ||
				currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.POSTPONED) ||
				currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.CLOSED))
		{
			if (!StringUtils.isEmpty(uwRiskCalcStatusReason))
			{
				String lookupType = currentCase.getUwCaseStatus().equals(UW_CASE_STATUS.CLOSED) ? "UW_CLOSEDOUT_REASON":"UW_DECLINE_REASON";
				lookup = findInLookup(lookupType, uwRiskCalcStatusReason);
				xml1 = new MarkupBuilder(writer);
				xml1.DecisionReason(tc:uwRiskCalcStatusReason, xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, StringUtils.upperCase(lookup.description));
				result.add(writer.toString());
				writer.getBuffer().setLength(0);
			}
		}
	}
	faceAmount = 0;
	requestedFaceAmount = 0;
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
					if (OPTION_VALUE_TYPE.FACE_AMOUNT == item.getProductPlanAvailability().getPlanOption().getValueType())
					{
						faceAmount = item.coverageAmount.setScale(0, BigDecimal.ROUND_FLOOR);
						
						if(requestedFaceAmount == 0){
							requestedFaceAmount=faceAmount;
						}
						break;
					}
				}
			}
		}
	}
	xml2 = new MarkupBuilder(writer);
	xml2.RequestedFaceAmt(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, requestedFaceAmount);
	result.add(writer.toString());
	writer.getBuffer().setLength(0);
	
	def ag_applicant_addtl_face = eformsAnswers.AG_ATTEST ? eformsAnswers.AG_ATTEST.ag_applicant_addtl_face : "";
	xml2 = new MarkupBuilder(writer);
	xml2.KeyedValue(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME){
		KeyName(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME,"ApplicantAddtlFace") 
		KeyValue(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, ag_applicant_addtl_face)
	};
	result.add(writer.toString());
	writer.getBuffer().setLength(0);
	
	def ag_applicant_addtl_face_amt = eformsAnswers.AG_ATTEST ? eformsAnswers.AG_ATTEST.ag_applicant_addtl_face_amt : null;
    if( ag_applicant_addtl_face_amt != null){
		xml2 = new MarkupBuilder(writer);
		xml2.KeyedValue(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME){
			KeyName(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME,"ApplicantAddtlFaceAmt")
			KeyValue(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, ag_applicant_addtl_face_amt)
		};
		result.add(writer.toString());
		writer.getBuffer().setLength(0);
	}

	def ag_applicant_exam_date = eformsAnswers.AG_ATTEST ? eformsAnswers.AG_ATTEST.ag_applicant_exam_date : null;
	if( ag_applicant_exam_date != null){
		xml2 = new MarkupBuilder(writer);
		xml2.KeyedValue(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME){
			KeyName(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME,"ApplicantExamDate")
			KeyValue(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, ag_applicant_exam_date)
		};
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

def int getUndoFinalActionCounter()
{
	String undoFinalActionCounterStr = currentCase.getPolicyNo().length()>8?currentCase.getPolicyNo().substring(8,currentCase.getPolicyNo().length()):"0";
	return Integer.parseInt(undoFinalActionCounterStr);
}

def List<String> getExtensionInfo()
{
	def result = [];
	String m7 = eformsAnswers.MED_REFLEXIVES.med_q30;
	String m7completer = eformsAnswers.MED_REFLEXIVES.medq30completer;
	writer = new StringWriter()
	
	if (m7)
	{
		String tcValue = (m7=="Y")?'1':'0';
		String value = (m7=="Y")? 'TRUE':'FALSE';
		xml = new MarkupBuilder(writer)
		xml.FutureTravelInd(tc:tcValue, xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, value);
		result.add(writer.toString());
		writer.getBuffer().setLength(0);
	}
	if (m7completer && m7 && m7 == "Y")
	{
		def completer = m7completer
		int i = 0
		while (eformsAnswers.MED_REFLEXIVES['medq30completer1___' + i])
		{
			completer += ", " + eformsAnswers.MED_REFLEXIVES['medq30completer1___' + i]
			i++
		}
		xml1 = new MarkupBuilder(writer);
		xml1.AdditionalInformation(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, completer);
		result.add(writer.toString());
	}
	return result;
}

def List<String> getDependentMedicalConditions()
{
	def result = [];
	String q1,q2;
	if(index==1){
		q1 = eformsAnswers.DEPENDENT.cMedQ1;
		q2 = eformsAnswers.DEPENDENT.cMedQ2;
	}else{
		q1 = eformsAnswers.DEPENDENT["cMedQ12___"+(index-2)];
		q2 = eformsAnswers.DEPENDENT["cMedQ22___"+(index-2)];		
	}
	result.add(q1);
	result.add(q2)
	return result;
}

def List<String> getTreatmentDescription() {
	def result = [];
	
	if(eformsAnswers.MED_REFLEXIVES)
	{
		String medq12completer = eformsAnswers.MED_REFLEXIVES.medq12completer;
		if(medq12completer) {
			writer = new StringWriter();
			xml = new MarkupBuilder(writer);
			xml.TreatmentDesc(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, medq12completer);
			result.add(writer.toString());
		}
		
		String addCondition0 = eformsAnswers.MED_REFLEXIVES.medq12completer1___0;
		if(addCondition0) {
			writer = new StringWriter();
			xml = new MarkupBuilder(writer);
			xml.TreatmentDesc(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, addCondition0);
			result.add(writer.toString());
		}
		
		String addCondition1 = eformsAnswers.MED_REFLEXIVES.medq12completer1___1;
		if(addCondition1) {
			writer = new StringWriter();
			xml = new MarkupBuilder(writer);
			xml.TreatmentDesc(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, addCondition1);
			result.add(writer.toString());
		}
		
		String addCondition2 = eformsAnswers.MED_REFLEXIVES.medq12completer1___2;
		if(addCondition2) {
			writer = new StringWriter();
			xml = new MarkupBuilder(writer);
			xml.TreatmentDesc(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, addCondition2);
			result.add(writer.toString());
		}
		
		String addCondition3 = eformsAnswers.MED_REFLEXIVES.medq12completer1___3;
		if(addCondition3) {
			writer = new StringWriter();
			xml = new MarkupBuilder(writer);
			xml.TreatmentDesc(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, addCondition3);
			result.add(writer.toString());
		}
		
		String addCondition4 = eformsAnswers.MED_REFLEXIVES.medq12completer1___4;
		if(addCondition4) {
			writer = new StringWriter();
			xml = new MarkupBuilder(writer);
			xml.TreatmentDesc(xmlns:XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME, addCondition4);
			result.add(writer.toString());
		}
	}

	return result;
}

def List<OutputStream> getRepeatingPdfContent()
{
	ByteArrayOutputStream ostream = new ByteArrayOutputStream();

	Object mappings = templateResource.getPDFMappings();

	List<OutputStream> pdfList = new ArrayList<OutputStream>();
	Map<String, String> fillableValues;
	Map<String, Object> replaced = expressionObjects.EFORMS.get(requirementCode);
	
	int i = 1;
	String replacedCompany = StringUtil.defaultIfEmpty(replaced.get("replaced_carrier___" + i), null);
	while (replacedCompany != null)
	{
		Map<String, String> theMappings = (Map<String, String>) mappings;
		for (Map.Entry<String, String> entry : theMappings.entrySet())
		{
			String key = entry.getKey();
			String value = entry.getValue();
			if (value.matches(".*___\\d.*"))
			{
				theMappings.put(key, value.replaceAll("___\\d", "___" + i));
			}
		}

		fillableValues = new GroovyHelper().evaluateExpressions(expressionObjects, theMappings,
				coreServices.getLookupService());
			
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		coreServices.getPdfService().fillPDF(new ByteArrayInputStream(templateResource.getContent()), os,
				fillableValues, signatures, barCodeField, barCode, text);

		IOUtils.copy(new ByteArrayInputStream(os.toByteArray()), ostream);
		byte[] pdf = os.toByteArray();
		if (pdf.length > 0)
		{
			pdfList.add(os);
		}
		i++;
		replacedCompany = StringUtil.defaultIfEmpty(replaced.get("replaced_carrier___" + i), null);
	}

	return pdfList;
}

def Boolean getSupplementalPdfContent() {
	Map<String,String> medReflexives = EFORMS.MED_REFLEXIVES
	List<Element> tables = getMedicalHistoryDetails(medReflexives)
	if (tables)
	{
		tables.findAll { pdfDocument.add(it) }
		return true
	}
	return false
}

def PdfPTable getDetailsTable(Map<String,String> reflexives, String questionId, String question) {
	if (reflexives.get(questionId) != null) {
		PdfPTable infoTable = new PdfPTable(1);
		infoTable.setSpacingBefore(AbstractItextReport.SPACING_BEFORE_TABLE);
		infoTable.setWidthPercentage(100f);
		infoTable.setHorizontalAlignment(Element.ALIGN_LEFT);
		infoTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
		
		if (question!=null)
		{
			infoTable.addCell(AbstractItextReport.labelCell(question));
		}
		if (questionId !=null)
		{
			infoTable.addCell(AbstractItextReport.valueCell(reflexives.get(questionId)));
		}
		
		return infoTable;
	}

	return null;
}

def List<PdfPTable> getMedicalHistoryDetails(Map<String,String> reflexives) {
	List<PdfPTable> tables = new ArrayList<PdfPTable>();
	PdfPTable details = getDetailsTable(reflexives, "otherwccompleter", 'Other Weight Change Reason');
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "medq17completer", QUESTIONS.MED_REFLEXIVES['med_q17']);
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "medq18completer", QUESTIONS.MED_REFLEXIVES['med_q18']);
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "medq20completer", QUESTIONS.MED_REFLEXIVES['med_q20']);
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "medq21completer", QUESTIONS.MED_REFLEXIVES['med_q21']);
	if (details != null)
	{
		tables.add(details);
	}	
	details = getDetailsTable(reflexives, "medq22completer", QUESTIONS.MED_REFLEXIVES['med_q22']);
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "medq23completer", QUESTIONS.MED_REFLEXIVES['med_q23']);
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "medq24completer", QUESTIONS.MED_REFLEXIVES['med_q24']);
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "medq25completer", QUESTIONS.MED_REFLEXIVES['med_q25']);
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "medq26completer", QUESTIONS.MED_REFLEXIVES['med_q26']);
	if (details != null)
	{
		tables.add(details);
	}
	details = getDetailsTable(reflexives, "med_q29", QUESTIONS.MED_REFLEXIVES['med_q29']);
	if (details != null)
	{
		if (reflexives.get("med_q29") == 'Y')
		{
			tables.add(details);
			tables.addAll(getFamilyHistoryContent(reflexives));
		}
	}
	details = getDetailsTable(reflexives, "med_q31_details", QUESTIONS.MED_REFLEXIVES['med_q31']);
	if (details != null)
	{
		tables.add(details);
	}
	
	if (tables.size() > 0)
	{
		tables.add(0, AbstractItextReport.sectionHeader("Medical History Supplemental Detail"));
	}

	return tables;
}

def List<PdfPTable> getFamilyHistoryContent(Map<String,String> reflexives) {
	List<PdfPTable> tables = new ArrayList<PdfPTable>();

	if (reflexives.get("family_disorder1") != null) {
		PdfPTable infoTable = new PdfPTable(8);
		infoTable.setSpacingBefore(AbstractItextReport.SPACING_BEFORE_TABLE);
		infoTable.setWidthPercentage(100f);
		infoTable.setHorizontalAlignment(Element.ALIGN_LEFT);
		infoTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
		
		infoTable.addCell(AbstractItextReport.labelCell("Family Disorder:"));
		infoTable.addCell(AbstractItextReport.valueCell(reflexives.get("family_disorder1")));
		infoTable.addCell(AbstractItextReport.labelCell("Relationship:"));
		infoTable.addCell(AbstractItextReport.valueCell(reflexives.get("family_relation1")));
		String status = reflexives.get("family_status1");
		String ageLabel = "Age Diagnosed:";
		if (status.equalsIgnoreCase("deceased")) {
			ageLabel = "Age at death:";
		}
		infoTable.addCell(AbstractItextReport.labelCell(ageLabel));
		infoTable.addCell(AbstractItextReport.valueCell(reflexives.get("family_age_of_death1")));
		infoTable.addCell(AbstractItextReport.labelCell("Family Member Status:"));
		infoTable.addCell(AbstractItextReport.valueCell(status));
		tables.add(infoTable);

		int i = 0;
		while (reflexives.get("family_disorder___" + i) != null) {
			infoTable = new PdfPTable(8);
			infoTable.setSpacingBefore(AbstractItextReport.SPACING_BEFORE_TABLE);
			infoTable.setWidthPercentage(100f);
			infoTable.setHorizontalAlignment(Element.ALIGN_LEFT);
			infoTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

			infoTable.addCell(AbstractItextReport.labelCell("Family Disorder:"));
			infoTable.addCell(AbstractItextReport.valueCell(StringUtil.defaultIfEmpty(reflexives.get("family_disorder___" + i), "")));
			infoTable.addCell(AbstractItextReport.labelCell("Relationship:"));
			infoTable.addCell(AbstractItextReport.valueCell(StringUtil.defaultIfEmpty(reflexives.get("family_relation___" + i), "")));
			status = StringUtil.defaultIfEmpty(reflexives.get("family_status___" + i), "");
			ageLabel = "Age Diagnosed:";
			if (status.equalsIgnoreCase("deceased")) {
				ageLabel = "Age at Death:";
			}
			infoTable.addCell(AbstractItextReport.labelCell(ageLabel));
			infoTable.addCell(AbstractItextReport.valueCell(StringUtil.defaultIfEmpty(reflexives.get("family_age_of_death___" + i), "")));
			infoTable.addCell(AbstractItextReport.labelCell("Family Member Status:"));
			infoTable.addCell(AbstractItextReport.valueCell(status));
			tables.add(infoTable);
			i++;
		}
	}

	return tables;
}

def Boolean isUndoFinalActionApplicable()
{
	return true;
}

def applyUndoFinalAction()
{
	SecurityOu ou = null;
	for (SecurityOu securityOu: securityOus)
	{
		if (securityOu.getOuCode().equalsIgnoreCase("FLIFE"))
		{
			ou = securityOu;
		}
	}
	currentCase.setOwner(ou, securityUser, true);
}

def void fillExtensionInfo()
{
	for (TXLifeRequestType txLifeRequestType: txLifeType.getTXLifeRequest())
	{
		PartyType partyType = modelAdapter.getDocElement("Primary_Insured_Party_1", PartyType.class, txLifeRequestType.getOLifE().getHoldingOrPartyOrRelation());
		RiskType riskType = partyType.getRisk();
		def result = getExtensionInfo();
		NBMessageAdapterUtil.createOLifeExtension(objectFactory, modelAdapter, vendorCode, result, riskType.getOLifEExtension());
	}
}

