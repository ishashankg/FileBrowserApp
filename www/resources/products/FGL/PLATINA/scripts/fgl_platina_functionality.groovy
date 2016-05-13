package FGL.PERFPRO.scripts;

import static com.stepsoln.core.db.cases.Case.CASE_STATUS.APPROVED
import static com.stepsoln.core.db.cases.Case.CASE_STATUS.ISSUED
import static com.stepsoln.core.db.cases.CaseActivity.ACTIVITY_CATEGORY.DECLINE_CASE
import static com.stepsoln.core.db.cases.CaseActivity.ACTIVITY_CATEGORY.EVAL_SUITABILITY
import static com.stepsoln.core.db.cases.CaseActivity.ACTIVITY_CATEGORY.FINAL_SUITABILITY
import static com.stepsoln.core.db.cases.CaseActivity.ACTIVITY_CATEGORY.SUITABILITY_RECOMMENDATION
import static com.stepsoln.core.db.cases.CaseActivity.ACTIVITY_CATEGORY.TAKE_FINAL_ACTION
import static com.stepsoln.core.db.cases.CaseActivity.ACTIVITY_CATEGORY.WITHDRAWN_CASE
import static com.stepsoln.core.db.enums.APPLICANT_TYPE.POLICY_OWNER
import static com.stepsoln.core.db.enums.APPLICANT_TYPE.JOINT_OWNER
import static com.stepsoln.core.db.enums.APPLICANT_TYPE.ANNUITANT
import static com.stepsoln.core.db.enums.APPLICANT_TYPE.JOINT_ANNUITANT
import static com.stepsoln.core.db.enums.APPLICANT_TYPE.OWNER_ANNUITANT
import static com.stepsoln.servicebus.api.XMLParseHelper.ACORD_103_XML_NAMESPACE_NAME

import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat
import java.util.List;
import java.util.stream.Collectors

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.collections.map.HashedMap
import org.apache.commons.lang.StringUtils

import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.db.cases.CaseActivity
import com.stepsoln.core.db.cases.CaseApplicationPayment
import com.stepsoln.core.db.cases.CaseCommentView
import com.stepsoln.core.db.cases.CaseUser
import com.stepsoln.core.db.cases.CaseActivity.ACTIVITY_CATEGORY
import com.stepsoln.core.db.enums.APPLICANT_TYPE
import com.stepsoln.core.db.uw.CaseUwRiskCalc
import com.stepsoln.core.db.cases.ValidationFailureItem
import com.stepsoln.core.generated.nb.OLifEType
import com.stepsoln.core.generated.nb.ApplicationInfoType
import com.stepsoln.core.generated.nb.FinancialActivityType
import com.stepsoln.core.generated.nb.HoldingType
import com.stepsoln.core.generated.nb.KeyedValueType
import com.stepsoln.core.generated.nb.OLILUREQCAT;
import com.stepsoln.core.generated.nb.ObjectFactory
import com.stepsoln.core.generated.nb.PERSISTKEY;
import com.stepsoln.core.generated.nb.PaymentType
import com.stepsoln.core.generated.nb.PolicyType
import com.stepsoln.core.generated.nb.PartyType
import com.stepsoln.core.generated.nb.RequirementInfoType;
import com.stepsoln.core.generated.nb.TXLifeRequestType
import com.stepsoln.core.generated.nb.TXLifeType
import com.stepsoln.core.generated.nb.SystemMessageType
import com.stepsoln.core.hibernate.StepHibernateTemplate
import com.stepsoln.core.util.DateUtil
import com.stepsoln.core.util.modeladapters.ModelAdapter
import com.stepsoln.core.util.modeladapters.NBMessageAdapterCommonUtil;
import com.stepsoln.core.util.modeladapters.NBMessageAdapterUtil
import com.stepsoln.core.db.modeladapters.SystemMessage
import com.stepsoln.core.db.security.SecurityUser;

def Case currentCase;
def Map<String, Map<String, Object>> eformsAnswers;
def ObjectFactory objectFactory;
def ModelAdapter modelAdapter;
def String vendorCode;
def TXLifeType txLifeType;
def StepHibernateTemplate hibernateTemplate;
def Map<String, String> groovyParams;
def List<CaseActivity> caseActivityList;
def List<CaseCommentView> commentList;
def List<ValidationFailureItem> validationFailureItemList;
def List<SystemMessage> systemMessages;

def String getSvcOu()
{
	return "CASE_MGR";
}

//TODO - dyao - this is testing only , need to take it out
def Boolean eformForcePageCompletion()
{
	return false;
}

def APPLICANT_TYPE defaultApplicantType()
{
	String ownerAnnuitant = eformsAnswers.PERSON.owner_Annuitant;
	if ("Y".equalsIgnoreCase(ownerAnnuitant))
	{
		return ANNUITANT;
	}
	else
	{
		return POLICY_OWNER;
	}
}

def getContractLocale()
{
	String contractStateCheck = eformsAnswers.PERSON.contract_state_check;
	if ("Y".equalsIgnoreCase(contractStateCheck))
	{
		return eformsAnswers.PERSON.state;
	}
	else
	{
		return eformsAnswers.PERSON.contract_state;
	}
}

def String getIssueNation()
{
	return getContractLocale();
}

def String getParticipantRoleCode()
{
	String ownerAnnuitant = eformsAnswers.PERSON.owner_Annuitant;
	if ("Y".equalsIgnoreCase(ownerAnnuitant))
	{
		return ANNUITANT.shortName;
	}
	else
	{
		return OWNER_ANNUITANT.shortName;
	}
}

def String getExistingInsuranceInd()
{
	if (defaultApplicantType() == APPLICANT_TYPE.valueOf(groovyParams.applicantType))
	{
		return eformsAnswers.REPLACEMENT.existing_policies;
	}
	else
	{
		return null;
	}
}

def setStatusChangeDate(PolicyType policyType)
{
	List<ACTIVITY_CATEGORY> activityCategoryList = [EVAL_SUITABILITY];
	List<CaseActivity> caseActivities = caseActivityList.stream().filter({
		caseActivity -> activityCategoryList.stream().anyMatch({activityCategory -> caseActivity.getActivityCategory().equals(activityCategory)})}).collect(Collectors.toList());
	if (CollectionUtils.isNotEmpty(caseActivities))
	{
		switch(caseActivities.get(caseActivities.size() - 1).getActivityCategory())
		{
			case EVAL_SUITABILITY:
				systemMessages.add(caseActivities.get(caseActivities.size() - 1));
		}
	}
	activityCategoryList = [WITHDRAWN_CASE, DECLINE_CASE, TAKE_FINAL_ACTION, SUITABILITY_RECOMMENDATION];
	caseActivities = caseActivityList.stream().filter({
		caseActivity -> activityCategoryList.stream().anyMatch({activityCategory -> caseActivity.getActivityCategory().equals(activityCategory)})}).collect(Collectors.toList());
	if (CollectionUtils.isNotEmpty(caseActivities))
	{
		caseActivities.each 
		{
			caseActivity ->
			switch(caseActivity.getActivityCategory()) 
			{
				case TAKE_FINAL_ACTION:
				if (currentCase.getStatus == APPROVED)
				{
					systemMessages.add(caseActivity);
				}
				case WITHDRAWN_CASE:
				case DECLINE_CASE:
					policyType.setStatusChangeDate(DateUtil.getShortXMLDate(caseActivity.getCreatedDate()));
					break;
				case SUITABILITY_RECOMMENDATION:
					systemMessages.add(caseActivity);
			}
		}
	}
}

def void addValidationFailureItems(OLifEType oLifeType, List<RequirementInfoType> requirementInfoTypeList)
{
	APPLICANT_TYPE applicantType = defaultApplicantType();
	if (CollectionUtils.isNotEmpty(validationFailureItemList))
	{
		validationFailureItemList.each
		{
			validationFailureItem ->
			RequirementInfoType requirementInfo = objectFactory.createRequirementInfoType();
			PERSISTKEY key = new PERSISTKEY();
			key.setValue(validationFailureItem.getValidationFailureItemId().toString());
			if (StringUtils.isNotBlank(validationFailureItem.getCategoryCode()))
			{
				key.setSystemCode(validationFailureItem.getCategoryCode());
			}
			requirementInfo.getRequirementInfoKey().add(key);
			requirementInfo.setReqCategory((OLILUREQCAT) modelAdapter.evaluate("RequirementCategory", validationFailureItem.getCategoryCode(), objectFactory.createOLILUREQCAT(), true));
			requirementInfo.setRequirementDetails(validationFailureItem.getDescription());
			SecurityUser securityUser = hibernateTemplate.get(SecurityUser.class, "where user_id=?", validationFailureItem.getCreatedBy());
			requirementInfo.setUserCode(securityUser.getUsername());
			if (validationFailureItem.getCreatedDate() != null)
			{
				requirementInfo.setRequestedDate(DateUtil.getShortXMLDate(validationFailureItem.getCreatedDate()));
			}
			if (validationFailureItem.getModifiedDate() != null)
			{
				requirementInfo.setFulfilledDate(DateUtil.getShortXMLDate(validationFailureItem.getModifiedDate()));
			}
			List<PartyType> defaultPartyTypes = modelAdapter.getDocElementList(applicantType.getCamelCase() + "_Party_", PartyType.class, oLifeType.getHoldingOrPartyOrRelation());
			String name = validationFailureItem.getName();
			List<PartyType> partyTypes = null;
			if (StringUtils.isNotBlank(name))
			{
				if (name.toUpperCase().indexOf("OWNER") != -1)
				{
					partyTypes = modelAdapter.getDocElementList(POLICY_OWNER.getCamelCase() + "_Party_", PartyType.class, oLifeType.getHoldingOrPartyOrRelation());
				}
				else if (name.toUpperCase().indexOf("JOINT_OWNER") != -1)
				{
					partyTypes = modelAdapter.getDocElementList(JOINT_OWNER.getCamelCase() + "_Party_", PartyType.class, oLifeType.getHoldingOrPartyOrRelation());
				}
				else if (name.toUpperCase().indexOf("ANNUITANT") != -1)
				{
					partyTypes = modelAdapter.getDocElementList(ANNUITANT.getCamelCase() + "_Party_", PartyType.class, oLifeType.getHoldingOrPartyOrRelation());
				}
				else if (name.toUpperCase().indexOf("JOINT_ANNUITANT") != -1)
				{
					partyTypes = modelAdapter.getDocElementList(JOINT_ANNUITANT.getCamelCase() + "_Party_", PartyType.class, oLifeType.getHoldingOrPartyOrRelation());
				}
			}
			if (CollectionUtils.isNotEmpty(partyTypes))
			{
				requirementInfo.setAppliesToPartyID(partyTypes.get(0));
			}
			else
			{
				requirementInfo.setAppliesToPartyID(defaultPartyTypes.get(0));
			}
			requirementInfoTypeList.add(requirementInfo);
		}
	}
}

def void fillAdditionalInfo()
{
	systemMessages = new ArrayList<SystemMessage>();
	List<CaseUser> caseUserList = currentCase.getCaseUsers();
	for (TXLifeRequestType txLifeRequestType: txLifeType.getTXLifeRequest())
	{
		OLifEType oLifeType = txLifeRequestType.getOLifE();
		HoldingType holdingType = modelAdapter.getDocElement("Holding_1", HoldingType.class, oLifeType.getHoldingOrPartyOrRelation());
		PolicyType policyType = holdingType.getPolicy();
		setStatusChangeDate(policyType);
		ApplicationInfoType applicationInfoType = policyType.getApplicationInfo();
		if (CollectionUtils.isNotEmpty(caseUserList))
		{
			CaseUser caseUserFound = caseUserList.find {caseUser -> caseUser.getSecurityOu() != null && "DEP".equalsIgnoreCase(caseUser.getSecurityOu().getOuCode())};
			applicationInfoType.setUserCode(caseUserFound.getSecurityUser().getUsername());
			applicationInfoType.setSubmissionDate(DateUtil.getShortXMLDate(caseUserFound.getCreatedDate()));
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
		if (StringUtils.isNotBlank(currentCase.getDescription()))
		{
			Date date = simpleDateFormat.parse(currentCase.getDescription());
			applicationInfoType.setHOReceiptDate(DateUtil.getShortXMLDate(date));
		}
		String appSignedDate = (String) modelAdapter.evaluate("AppSignedDate", null, null, false);
		if (StringUtils.isNotBlank(appSignedDate))
		{
			Date date = simpleDateFormat.parse(appSignedDate);
			applicationInfoType.setSignedDate(DateUtil.getShortXMLDate(date));
		}
		systemMessages.addAll(commentList);
		NBMessageAdapterUtil.createSystemMessage(objectFactory, modelAdapter, holdingType, systemMessages);
		addValidationFailureItems(oLifeType, policyType.getRequirementInfo());
	}
	fillExtensionInfo();
	fillKeyedValue();
}

def String getReplacementCode()
{
	String existingPolicies = eformsAnswers.REPLACEMENT.existing_policies;
	String replacePolicies = eformsAnswers.REPLACEMENT.replace_policies;
	String replacementCode = "1";
	if (("Y".equalsIgnoreCase(existingPolicies)) && ("Y".equalsIgnoreCase(replacePolicies)))
	{
		String returnValue = modelAdapter.evaluate("ReplacementCodeType", null, null, false);
		if (StringUtils.isNotEmpty(returnValue))
		{
			String [] values = returnValue.split(",");
			boolean internal = values.any {value -> "INTERNAL".equalsIgnoreCase(value)};
			boolean external = values.any {value -> "EXTERNAL".equalsIgnoreCase(value)};
			if (internal && external) 
			{
				replacementCode = "4";
			}
			else if (internal)
			{
				replacementCode = "2";
			}
			else if (external)
			{
				replacementCode = "3";
			}
			else
			{
				replacementCode = "0";
			}
		}
	}
	return replacementCode;
}

def Boolean holdingSysKey()
{
	return false;
}

def Boolean addCompaign()
{
	return false;
}

def Boolean addInvestment()
{
	return false;
}

def void paymentExtensionInfo(HoldingType holdingType)
{
	List<CaseApplicationPayment> caseApplicationPayments = currentCase.getPayments();
	if (CollectionUtils.isNotEmpty(caseApplicationPayments))
	{
		caseApplicationPayments.each {
			caseApplicationPayment ->
			String paymentId = "Previous_Payment_" + caseApplicationPayment.getCaseApplicationPaymentId();
			List<FinancialActivityType> financialActivityTypes = holdingType.getPolicy().getFinancialActivity();
			financialActivityTypes.each {
				financialActivityType ->
				if (paymentId.equalsIgnoreCase(financialActivityType.getId()))
				{
					def result = [];
					writer = new StringWriter();
					xml1 = new MarkupBuilder(writer);
					if (caseApplicationPayment.getPreTEFRACostBasis() != null || caseApplicationPayment.getPostTEFRACostBasis() != null)
					{
						xml1.CostBasisFlag(xmlns:ACORD_103_XML_NAMESPACE_NAME, "Y");
					}
					else
					{
						xml1.CostBasisFlag(xmlns:ACORD_103_XML_NAMESPACE_NAME, "N");
					}
					result.add(writer.toString());
					writer.getBuffer().setLength(0);
					NBMessageAdapterUtil.createOLifeExtension(objectFactory, modelAdapter, vendorCode, result, financialActivityType.getOLifEExtension());
				}
			}
		}
	}
}

def void systemMessageExtensionInfo(HoldingType holdingType)
{
	if (CollectionUtils.isNotEmpty(systemMessages))
	{
		systemMessages.each {
			systemMessage -> 
			String id = "System_Message_" + String.valueOf(systemMessage.getId());
			List<SystemMessageType> systemMessageTypes = holdingType.getSystemMessage();
			systemMessageTypes.each {
				systemMessageType ->
				if (id.equalsIgnoreCase(systemMessageType.getId()))
				{
					def result = [];
					writer = new StringWriter();
					xml1 = new MarkupBuilder(writer);
					if (StringUtils.isNotEmpty(systemMessage.getType()))
					{
						xml1.MessageType(xmlns:ACORD_103_XML_NAMESPACE_NAME, systemMessage.getType());
					}
					result.add(writer.toString());
					writer.getBuffer().setLength(0);
					NBMessageAdapterUtil.createOLifeExtension(objectFactory, modelAdapter, vendorCode, result, systemMessageType.getOLifEExtension());
				}
			}
		}
	}
}

def void fillExtensionInfo()
{
	for (TXLifeRequestType txLifeRequestType: txLifeType.getTXLifeRequest())
	{
		List<HoldingType> holdingTypes = modelAdapter.getDocElementList("Prev_Holding_", HoldingType.class, txLifeRequestType.getOLifE().getHoldingOrPartyOrRelation());
		for (HoldingType holdingType: holdingTypes)
		{
			paymentExtensionInfo(holdingType);
		}
		HoldingType holdingType = modelAdapter.getDocElement("Holding_1", HoldingType.class, txLifeRequestType.getOLifE().getHoldingOrPartyOrRelation());
		systemMessageExtensionInfo(holdingType);
	}
}

def void paymentKeyedValue(HoldingType holdingType)
{
	List<CaseApplicationPayment> caseApplicationPayments = currentCase.getPayments();
	if (CollectionUtils.isNotEmpty(caseApplicationPayments))
	{
		caseApplicationPayments.each {
			caseApplicationPayment ->
			String paymentId = "Payment_" + caseApplicationPayment.getCaseApplicationPaymentId();
			List<FinancialActivityType> financialActivityTypes = holdingType.getPolicy().getFinancialActivity();
			financialActivityTypes.each {
				financialActivityType ->
				financialActivityType.getPayment().each {
					paymentType -> 
					if (paymentId.equalsIgnoreCase(paymentType.getId()))
					{
						Map<String, String> keyedValueMap = new HashedMap<String, String>();
						keyedValueMap.put("PreTEFRAGain", String.valueOf(caseApplicationPayment.getPreTEFRAGain().doubleValue()));
						keyedValueMap.put("PostTEFRAGain", String.valueOf(caseApplicationPayment.getPostTEFRAGain().doubleValue()));
						List<KeyedValueType> keyedValueTypes = NBMessageAdapterUtil.createKeyedValue(objectFactory, keyedValueMap, null);
						paymentType.getKeyedValue().addAll(keyedValueTypes);
					}
				}
			}
		}
	}
}

def void policyKeyedValue(HoldingType holdingType)
{
	PolicyType policyType = holdingType.getPolicy();
	Map<String, String> keyedValueMap = new HashedMap<String, String>();
	CaseUwRiskCalc latestRiskCalc = currentCase.getLatestRiskCalc();
	if ((latestRiskCalc != null) && StringUtils.isNotEmpty(latestRiskCalc.getUwStatus()))
	{
		keyedValueMap.put("SuitabilityStatus", latestRiskCalc.getUwStatus());
	}
	List<CaseUser> caseUserList = currentCase.getCurrentCaseUserList();
	if (CollectionUtils.isNotEmpty(caseUserList))
	{
		caseUserList.each {
			caseUser ->
			if (caseUser.getSecurityOu() != null && "CASE_MGR".equalsIgnoreCase(caseUser.getSecurityOu().getOuCode()))
			{
				keyedValueMap.put("CaseManagerId", caseUser.getSecurityUser().getUsername());
			}
		}
	}
	String jointOwner = eformsAnswers.PERSON.joint_owner;
	if ("Y".equalsIgnoreCase(jointOwner))
	{
		keyedValueMap.put("MultipleLivesInd", "True");
	}
	else
	{
		String ownerAnnuitant = eformsAnswers.PERSON.owner_Annuitant;
		if ("Y".equalsIgnoreCase(ownerAnnuitant))
		{
			String jointAnnuitant = eformsAnswers.ANNUITANT.joint_annuitant;
			if ("Y".equalsIgnoreCase(jointAnnuitant))
			{
				keyedValueMap.put("MultipleLivesInd", "True");
			}
			else
			{
				keyedValueMap.put("MultipleLivesInd", "False");
			}
		}
		else
		{
			keyedValueMap.put("MultipleLivesInd", "False");
		}
	}
	List<KeyedValueType> keyedValueTypes = NBMessageAdapterUtil.createKeyedValue(objectFactory, keyedValueMap, null);
	policyType.getKeyedValue().addAll(keyedValueTypes);
}

def void fillKeyedValue()
{
	for (TXLifeRequestType txLifeRequestType: txLifeType.getTXLifeRequest())
	{
		HoldingType holdingType = modelAdapter.getDocElement("Holding_1", HoldingType.class, txLifeRequestType.getOLifE().getHoldingOrPartyOrRelation());
		paymentKeyedValue(holdingType);
		policyKeyedValue(holdingType);
	}
}

def String getHOAppFormNumber()
{
	def contractLocale = getContractLocale();
	if (["CT","NC","OK","VT"].contains(contractLocale))
	{
		return "ADV1070";
	}
	else if (["NV"].contains(contractLocale))
	{
		return "ADV1062";
	}
	else if (["MD"].contains(contractLocale)) 
	{
		return "ADV1361";
	}
	return "ADV1010";
}

def String getPolicyStatus()
{
	return currentCase.getStatus().name();
}
