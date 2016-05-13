function createAddress(address1, address2, address3, address4)
{
	var fullAddress = "";
	
	if ((address1 != null) && (address1 != "")) 
	{
		fullAddress = address1;
	}
	
	if ((address2 != null) && (address2 != "")) 
	{
		fullAddress = fullAddress + "\n" + address2;
	}
	
	if ((address3 != null) && (address3 != "")) 
	{
		fullAddress = fullAddress + "\n" + address3;
	}
	
	if ((address4 != null) && (address4 != "")) 
	{
		fullAddress = fullAddress + "\n" + address4;
	}
	
	return fullAddress.toUpperCase();
}


function createName(firstName, middleName, lastName, subName)
{
	var fullName = "";
	
	if ((firstName != null) && (firstName != "")) 
	{
		fullName = firstName;
	}
	
	if ((middleName != null) && (middleName != "")) 
	{
		fullName = fullName + " " + middleName;
	}
	
	if ((lastName != null) && (lastName != "")) 
	{
		fullName = fullName + " " + lastName;
	}
	
	if ((subName != null) && (subName != "")) 
	{
		fullName = fullName + " " + subName;
	}
	
	return fullName.toUpperCase();
}
