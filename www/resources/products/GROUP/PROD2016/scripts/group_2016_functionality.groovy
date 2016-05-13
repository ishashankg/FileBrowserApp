package HFA.scripts;

import groovy.xml.MarkupBuilder
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.WordUtils
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
import com.stepsoln.core.db.carrier.Carrier
import com.stepsoln.core.db.carrier.CarrierSequence.SEQUENCE_TYPE
import com.stepsoln.core.db.cases.Applicant
import com.stepsoln.core.db.cases.ApplicationQuote
import com.stepsoln.core.db.cases.ApplicationQuoteItem
import com.stepsoln.core.db.cases.Case
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
import com.stepsoln.core.db.product.Product
import com.stepsoln.core.db.product.ProductPlanAvailability
import com.stepsoln.core.db.security.SecurityInvitation
import com.stepsoln.core.db.security.SecurityOu
import com.stepsoln.core.db.security.SecurityUser
import com.stepsoln.core.db.services.CoreServices
import com.stepsoln.core.db.services.RequirementsService
import com.stepsoln.core.db.services.SequenceService
import com.stepsoln.core.db.services.util.DialogButtonConfig
import com.stepsoln.core.db.services.util.DialogConfig
import com.stepsoln.core.db.services.util.UserInterfaceFacesBehavior
import com.stepsoln.core.eform.EFormAnswers
import com.stepsoln.core.eform.EFormTransferHelper
import com.stepsoln.core.generated.nb.FamilyIllnessType
import com.stepsoln.core.generated.nb.OLILUREL
import com.stepsoln.core.hibernate.StepHibernateTemplate
import com.stepsoln.core.util.DateUtil
import com.stepsoln.core.util.QuickMap
import com.stepsoln.core.util.SystemConfiguration
import com.stepsoln.core.util.TemplateHelper
import com.stepsoln.core.util.modeladapters.NBToCaseAdapter
import com.stepsoln.core.util.pdfreports.AbstractItextReport
import com.stepsoln.core.util.pdfreports.PdfUtil
import com.stepsoln.servicebus.api.EmailRequestType
import com.stepsoln.servicebus.api.XMLParseHelper

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

def String getPolicyStatus()
{
	return currentCase.getStatus().name();
}

def Boolean isUniSexRates()
{
	return false;
}

def APPLICANT_TYPE defaultApplicantType()
{
	return APPLICANT_TYPE.PRIMARY_INSURED
}