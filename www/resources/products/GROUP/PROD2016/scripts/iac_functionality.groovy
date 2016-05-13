package forms.FLAST.scripts;

import org.apache.camel.ProducerTemplate
import org.slf4j.Logger
import com.stepsoln.core.db.carrier.Carrier
import com.stepsoln.core.db.cases.Applicant
import com.stepsoln.core.db.party.Agent
import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.db.cases.CaseRequirement;
import com.stepsoln.core.db.cases.CaseRequirement.LOCK_STATUS;
import com.stepsoln.core.db.config.ConfigResource
import com.stepsoln.core.db.security.SecurityInvitation
import com.stepsoln.core.db.security.SecurityInvitation.INVITATION_STATUS
import com.stepsoln.core.db.services.CoreServices
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.util.QuickMap
import com.stepsoln.core.util.RandomUtil
import com.stepsoln.core.util.SystemConfiguration
import com.stepsoln.core.util.TemplateHelper
import com.stepsoln.core.util.XMLUtil
import com.stepsoln.core.util.StepXMLHelper
import com.stepsoln.servicebus.api.EmailRequestType
import com.stepsoln.servicebus.api.ParameterType;
import com.stepsoln.servicebus.api.StepRequestType
import com.stepsoln.servicebus.api.ObjectFactory
import org.apache.commons.lang.StringUtils
import com.stepsoln.core.db.cases.ApplicationQuote
import com.stepsoln.core.db.cases.ApplicationQuoteItem
import org.apache.commons.beanutils.PropertyUtils
import com.stepsoln.core.db.product.PlanOption.OPTION_VALUE_TYPE
import java.math.BigDecimal
import java.util.Date;

import com.stepsoln.core.db.product.ProductPlanAvailability
import com.stepsoln.core.util.modeladapters.CaseToNbMessageAdapter
import com.stepsoln.servicebus.api.HeaderVariable

def Case currentCase;
def ApplicationQuote applicationQuote;
def Logger logger;
def Services services;
def CoreServices coreServices;
def ProducerTemplate producerTemplate;
def Map<String, Map<String, Object>> eformsAnswers;

def String getOccupation()
{
	eforms = eformsAnswers.PRECOVERAGE;
	return eforms.jobtitle;
}

def String getMaritalStatus()
{
	eforms = eformsAnswers.PRECOVERAGE;
	return eforms.maritalstatus;
}

