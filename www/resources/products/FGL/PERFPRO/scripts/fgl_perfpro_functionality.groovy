package FGL.PERFPRO.scripts;

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.collections.map.HashedMap

import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.db.cases.CaseApplicationPayment
import com.stepsoln.core.db.enums.APPLICANT_TYPE
import com.stepsoln.core.generated.nb.FinancialActivityType
import com.stepsoln.core.generated.nb.HoldingType
import com.stepsoln.core.generated.nb.KeyedValueType
import com.stepsoln.core.generated.nb.ObjectFactory
import com.stepsoln.core.generated.nb.PaymentType
import com.stepsoln.core.generated.nb.TXLifeRequestType
import com.stepsoln.core.generated.nb.TXLifeType
import com.stepsoln.core.util.modeladapters.ModelAdapter
import com.stepsoln.core.util.modeladapters.NBMessageAdapterUtil

def Case currentCase;
def Map<String, Map<String, Object>> eformsAnswers;
def ObjectFactory objectFactory;
def ModelAdapter modelAdapter;
def String vendorCode;
def TXLifeType txLifeType;

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
		return APPLICANT_TYPE.ANNUITANT;
	}
	else
	{
		return APPLICANT_TYPE.POLICY_OWNER;
	}
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

def String getIssueNation()
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

def String getParticipantRoleCode()
{
	String ownerAnnuitant = eformsAnswers.PERSON.owner_Annuitant;
	if ("Y".equalsIgnoreCase(ownerAnnuitant))
	{
		return APPLICANT_TYPE.ANNUITANT.shortName;
	}
	else
	{
		return APPLICANT_TYPE.OWNER_ANNUITANT.shortName;
	}
}

def void fillExtensionInfo()
{
}

def void fillKeyedValue()
{
	List<CaseApplicationPayment> caseApplicationPayments = currentCase.getPayments();
	if (CollectionUtils.isNotEmpty(caseApplicationPayments))
	{
		for (TXLifeRequestType txLifeRequestType: txLifeType.getTXLifeRequest())
		{
			for (CaseApplicationPayment caseApplicationPayment: caseApplicationPayments)
			{
				String paymentId = "Payment_" + caseApplicationPayment.getCaseApplicationPaymentId();
				HoldingType holdingType = modelAdapter.getDocElement("Holding_1", HoldingType.class, txLifeRequestType.getOLifE().getHoldingOrPartyOrRelation());
				List<FinancialActivityType> financialActivityTypes = holdingType.getPolicy().getFinancialActivity();
				for (FinancialActivityType financialActivityType: financialActivityTypes)
				{
					for (PaymentType paymentType: financialActivityType.getPayment())
					{
						if (paymentId.equalsIgnoreCase(paymentType.getId()))
						{
							Map<String, BigDecimal> keyedValueMap = new HashedMap<String, BigDecimal>();
							keyedValueMap.put("PreTEFRAGain", caseApplicationPayment.getPreTEFRAGain());
							keyedValueMap.put("PostTEFRAGain", caseApplicationPayment.getPostTEFRAGain());
							List<KeyedValueType> keyedValueTypes = NBMessageAdapterUtil.createKeyedValue(objectFactory, keyedValueMap, null);
							paymentType.getKeyedValue().addAll(keyedValueTypes);
						}
					}
				}
			}
		}
	}
}

def String getHOAppFormNumber()
{
	def contractLocale = [];
	String contractStateCheck = eformsAnswers.PERSON.contract_state_check;
	if ("Y".equalsIgnoreCase(contractStateCheck))
	{
		contractLocale = eformsAnswers.PERSON.state;
	}
	else
	{
		contractLocale = eformsAnswers.PERSON.contract_state;
	}
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
	return null;
}

def String getPolicyStatus()
{
	return currentCase.getStatus().name();
}
